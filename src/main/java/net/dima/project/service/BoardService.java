package net.dima.project.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dima.project.dto.BoardDTO;
import net.dima.project.entity.BoardEntity;
import net.dima.project.repository.BoardRepository;
import net.dima.project.repository.UserRepository;
import net.dima.project.util.FileService;
import net.dima.project.util.Masking;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${user.board.pageLimit}")
    int pageLimit;

    @Value("${spring.servlet.multipart.location}")
    String uploadPath;

    /**
     * 게시글 목록 조회 (페이징 + 검색 + 이름 마스킹 적용)
     */
    public Page<BoardDTO> selectAll(Pageable pageable, String searchCategory, String searchKeyword) {
        // 컨트롤러가 0-based Pageable을 만들어 넘김. 여기선 그대로 사용.
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize() > 0 ? pageable.getPageSize() : pageLimit;

        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "boardSeq"));
        Page<BoardEntity> temp;

        String kw = (searchKeyword == null) ? "" : searchKeyword.trim();

        switch (searchCategory) {
            case "boardTitle":
                temp = boardRepository.findByBoardTitleContains(kw, pr);
                break;
            case "boardWriter":
                // "작성자"는 실명으로 들어오므로, 이름 → 아이디 목록 변환 후 IN 검색
                if (kw.isEmpty()) {
                    temp = boardRepository.findAll(pr);
                } else {
                    var ids = userRepository.findUserIdsByUserNameLike(kw);
                    temp = ids.isEmpty() ? Page.empty(pr) : boardRepository.findByBoardWriterIn(ids, pr);
                }
                break;
            case "boardContent":
                temp = boardRepository.findByBoardContentContains(kw, pr);
                break;
            default:
                temp = boardRepository.findAll(pr);
                break;
        }

        // user_id → user_name 조회 후 마스킹된 DTO 반환
        return temp.map(board -> {
            String userId = board.getBoardWriter();
            String userName = userRepository.findById(userId)
                    .map(user -> user.getUserName())
                    .orElse("알수없음");

            // boardStatus 보정
            board.setBoardStatus(board.getReply() != null ? "답변완료" : "진행중");

            return BoardDTO.toDTO(board, userName);
        });
    }

    /** 게시글 등록 */
    public void insertBoard(BoardDTO boardDTO) {
        String originalFilename = null;
        String savedFilename = null;

        if (boardDTO.getUploadFile() != null && !boardDTO.getUploadFile().isEmpty()) {
            originalFilename = boardDTO.getUploadFile().getOriginalFilename();
            savedFilename = FileService.saveFile(boardDTO.getUploadFile(), uploadPath);
            boardDTO.setOriginalFilename(originalFilename);
            boardDTO.setSavedFilename(savedFilename);
        }

        BoardEntity boardEntity = BoardEntity.toEntity(boardDTO);
        boardRepository.save(boardEntity);
    }

    /** 게시글 상세 조회 */
    public BoardDTO selectOne(Long boardSeq) {
        Optional<BoardEntity> temp = boardRepository.findById(boardSeq);
        BoardDTO boardDTO = null;

        if (temp.isPresent()) {
            BoardEntity entity = temp.get();
            boardDTO = BoardDTO.toDTO(entity); // 상세는 마스킹 없이
        }
        if (boardDTO == null)
            return null;

        String boardWriter = boardDTO.getBoardWriter(); // user_id
        String writerName = userService.findRealNameById(boardWriter);
        boardDTO.setMaskedWriterName(Masking.maskUserName(writerName));

        return boardDTO;
    }

    /** 조회수 증가 */
    @Transactional
    public void incrementHitcount(Long boardSeq) {
        boardRepository.findById(boardSeq).ifPresent(entity -> entity.setHitCount(entity.getHitCount() + 1));
    }

    /** 게시글 삭제 (첨부파일 포함) */
    public void deleteOne(Long boardSeq) {
        Optional<BoardEntity> temp = boardRepository.findById(boardSeq);
        if (temp.isEmpty())
            return;

        BoardEntity entity = temp.get();
        String savedFilename = entity.getSavedFilename();

        log.info("삭제할 파일명: {}", savedFilename);

        if (savedFilename != null) {
            String fullPath = uploadPath + "/" + savedFilename;
            FileService.deleteFile(fullPath);
        }

        boardRepository.deleteById(boardSeq);
    }

    /** 게시글 수정 */
    @Transactional
    public void updateBoard(BoardDTO boardDTO) {
        MultipartFile uploadFile = boardDTO.getUploadFile();

        String originalFilename = null;
        String savedFilename = null;
        String oldSavedFilename = null;

        if (uploadFile != null && !uploadFile.isEmpty()) {
            originalFilename = uploadFile.getOriginalFilename();
            savedFilename = FileService.saveFile(uploadFile, uploadPath);
        }

        Long boardSeq = boardDTO.getBoardSeq();
        Optional<BoardEntity> temp = boardRepository.findById(boardSeq);

        if (temp.isPresent()) {
            BoardEntity entity = temp.get();
            oldSavedFilename = entity.getSavedFilename();

            if (oldSavedFilename != null && uploadFile != null && !uploadFile.isEmpty()) {
                String fullPath = uploadPath + "/" + oldSavedFilename;
                FileService.deleteFile(fullPath);
                entity.setOriginalFilename(originalFilename);
                entity.setSavedFilename(savedFilename);
            } else if (oldSavedFilename == null && uploadFile != null && !uploadFile.isEmpty()) {
                entity.setOriginalFilename(originalFilename);
                entity.setSavedFilename(savedFilename);
            }

            entity.setBoardTitle(boardDTO.getBoardTitle());
            entity.setBoardContent(boardDTO.getBoardContent());
            entity.setUpdateDate(LocalDateTime.now());
        }
    }
}
