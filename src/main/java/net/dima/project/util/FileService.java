package net.dima.project.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileService {

   public static String saveFile(MultipartFile uploadFile, String uploadPath) {

      if (uploadFile == null || uploadFile.isEmpty()) {
         return null;
      }

      try {
         // 저장 디렉토리 생성
         Path directory = Paths.get(uploadPath);
         if (!Files.exists(directory)) {
            Files.createDirectories(directory);
         }

         String originalFileName = uploadFile.getOriginalFilename();
         if (originalFileName == null) {
            originalFileName = "unknown";
         }

         String filename;
         String ext;
         String uuid = UUID.randomUUID().toString(); // 난수

         // .의 위치 찾기
         int position = originalFileName.lastIndexOf(".");

         if (position == -1) { // 확장자가 없는 파일
            ext = "";
            filename = originalFileName;
         } else {
            ext = originalFileName.substring(position); // includes dot
            filename = originalFileName.substring(0, position);
         }

         String savedFileName = filename + "_" + uuid + ext;

         // 디렉토리에 저장하기
         Path serverFile = directory.resolve(savedFileName);
         uploadFile.transferTo(serverFile.toFile());

         return savedFileName;
      } catch (IOException e) { // 저장장치에 저장이 안된것이므로, DB도 저장하면 안됨
         log.error("파일 저장 중 오류 발생: {}", e.getMessage(), e);
         return null;
      }
   }

   // 파일 삭제
   public static boolean deleteFile(String fullPath) {
      if (fullPath == null || fullPath.trim().isEmpty()) {
         return false;
      }

      try {
         Path filePath = Paths.get(fullPath);
         return Files.deleteIfExists(filePath);
      } catch (IOException e) {
         log.error("파일 삭제 중 오류 발생 (경로: {}): {}", fullPath, e.getMessage(), e);
         return false;
      }
   }
}
