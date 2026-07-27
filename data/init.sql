-- =====================================================================
-- 해야지(haeyaji) 스키마 (MySQL 8)
--
-- ⚠️ 이 파일은 "실제 실행 중인 앱 스키마"에서 생성한다(코드 = 스키마 100% 일치 보장).
--    갱신법: docker exec haeyaji-mysql mysqldump -uroot -p --no-data --skip-comments haeyaji_db
--            → users(리팩터 잔재)·todo.category(엔티티 없음) 등 dead 요소 제거 후 교체
--    직접 손으로 고치지 말 것 — 손편집본은 엔티티와 어긋나 첫 로그인 INSERT가 깨진 이력이 있다
--    (nickname NOT NULL / kakao_id·friend_code NOT NULL 등, #61).
--
-- 참고: 컬럼 설명·설계 의도는 docs/haeyaji-erd.dbml, docs/haeyaji-schema.sql 참조(문서 전용).
-- =====================================================================

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend` (
  `id` binary(16) NOT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `accepted_at` datetime(6) DEFAULT NULL,
  `receiver_id` binary(16) NOT NULL,
  `requester_id` binary(16) NOT NULL,
  `status` enum('ACCEPTED','PENDING','REJECTED') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `label` (
  `id` binary(16) NOT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `updatedAt` datetime(6) DEFAULT NULL,
  `color` varchar(20) DEFAULT NULL,
  `member_id` binary(16) DEFAULT NULL,
  `name` varchar(30) NOT NULL,
  `version` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting` (
  `id` binary(16) NOT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `confirmed_end_at` datetime(6) DEFAULT NULL,
  `confirmed_start_at` datetime(6) DEFAULT NULL,
  `creator_id` binary(16) NOT NULL,
  `deadline` datetime(6) DEFAULT NULL,
  `share_token` varchar(64) NOT NULL,
  `slot_unit_minutes` int NOT NULL,
  `status` enum('COLLECTING','CONFIRMED','EXPIRED') NOT NULL,
  `time_end` time NOT NULL,
  `time_start` time NOT NULL,
  `title` varchar(100) NOT NULL,
  `type` enum('CASUAL','ETC','REGULAR','TEAM') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK99eko91b44nxh2xa4o4v5besh` (`share_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_date` (
  `id` binary(16) NOT NULL,
  `candidate_date` date NOT NULL,
  `meeting_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_participant` (
  `id` binary(16) NOT NULL,
  `joined_at` datetime(6) NOT NULL,
  `meeting_id` binary(16) NOT NULL,
  `member_id` binary(16) NOT NULL,
  `invite_status` enum('ACCEPTED','PENDING','REJECTED') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_response` (
  `id` binary(16) NOT NULL,
  `meeting_time_slot_id` binary(16) NOT NULL,
  `member_id` binary(16) NOT NULL,
  `status` enum('BUSY','FREE') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `meeting_time_slot` (
  `id` binary(16) NOT NULL,
  `meeting_id` binary(16) NOT NULL,
  `slot_start_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `id` binary(16) NOT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `updatedAt` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `role` enum('ROLE_USER') NOT NULL,
  `social_type` enum('GOOGLE','KAKAO','NAVER') NOT NULL,
  `social_type_id` varchar(255) NOT NULL,
  `status` enum('ACTIVE','WITHDRAWN') NOT NULL,
  `withdrawn_at` datetime(6) DEFAULT NULL,
  `nickname` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhbulg32ie2c340v69qk8oi35l` (`social_type`,`social_type_id`),
  UNIQUE KEY `UKhh9kg6jti4n1eoiertn2k6qsc` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_category_weight` (
  `category` enum('CAFE_DESSERT','CULTURE_EXHIBIT','INDOOR_PLAY','NATURE_WALK','RESTAURANT','REST_HEALING','SHOPPING','SOCIAL','SPORTS_ACTIVITY','STUDY_WORK') NOT NULL,
  `ctx_time_of_day` enum('AFTERNOON','EVENING','MORNING','NIGHT') NOT NULL,
  `ctx_weather` enum('CLEAR','RAINY') NOT NULL,
  `member_id` binary(16) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `weight` double NOT NULL,
  PRIMARY KEY (`category`,`ctx_time_of_day`,`ctx_weather`,`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_keyword_weight` (
  `keyword` varchar(50) NOT NULL,
  `member_id` binary(16) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `weight` double NOT NULL,
  PRIMARY KEY (`keyword`,`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_preference` (
  `id` binary(16) NOT NULL,
  `avoid` json DEFAULT NULL,
  `intensity` varchar(20) DEFAULT NULL,
  `preferred_categories` json DEFAULT NULL,
  `vibe` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `id` binary(16) NOT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `body` varchar(255) DEFAULT NULL,
  `category` enum('FRIEND','INVITE','TODO') NOT NULL,
  `link_token` varchar(64) DEFAULT NULL,
  `member_id` binary(16) NOT NULL,
  `is_read` bit(1) NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `ref_id` binary(16) DEFAULT NULL,
  `title` varchar(100) NOT NULL,
  `type` enum('FRIEND_REQUEST','FRIEND_RESPONSE','MEETING_CONFIRMED','MEETING_INVITE','MEETING_INVITE_RESPONSE','MEETING_REMINDER','SHARE_INVITE','SHARE_INVITE_RESPONSE','TODO_REMINDER','TODO_SHARED_UPDATED','TODO_WEATHER_ALERT') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_noti_idem` (`member_id`,`type`,`ref_id`),
  KEY `idx_noti_inbox` (`member_id`,`id`),
  KEY `idx_noti_unread` (`member_id`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `routine` (
  `id` binary(16) NOT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `updatedAt` datetime(6) DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `label_id` binary(16) DEFAULT NULL,
  `member_id` binary(16) DEFAULT NULL,
  `start_time` time DEFAULT NULL,
  `title` varchar(100) NOT NULL,
  `version` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `routine_day` (
  `id` binary(16) NOT NULL,
  `day_of_week` varchar(3) NOT NULL,
  `routine_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKevx6r2ps0l7pk5xanqynfiywc` (`routine_id`),
  CONSTRAINT `FKevx6r2ps0l7pk5xanqynfiywc` FOREIGN KEY (`routine_id`) REFERENCES `routine` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `todo` (
  `id` binary(16) NOT NULL,
  `createdAt` datetime(6) DEFAULT NULL,
  `updatedAt` datetime(6) DEFAULT NULL,
  `ended_at` datetime(6) DEFAULT NULL,
  `lat` double DEFAULT NULL,
  `lng` double DEFAULT NULL,
  `pinned` bit(1) NOT NULL,
  `place_name` varchar(100) DEFAULT NULL,
  `place_url` varchar(300) DEFAULT NULL,
  `sort_order` int NOT NULL,
  `source` enum('AI','MANUAL','MEETING','ROUTINE') NOT NULL,
  `source_ref_id` binary(16) DEFAULT NULL,
  `start_time` time DEFAULT NULL,
  `status` enum('DONE','TODO') NOT NULL,
  `title` varchar(100) NOT NULL,
  `todo_date` date NOT NULL,
  `label_id` binary(16) DEFAULT NULL,
  `member_id` binary(16) DEFAULT NULL,
  `version` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_todo_routine_dedup` (`todo_date`,`source`,`source_ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `todo_participant` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `invite_status` enum('ACCEPTED','PENDING','REJECTED') NOT NULL,
  `member_id` binary(16) NOT NULL,
  `role` enum('EDITOR','OWNER','VIEWER') NOT NULL,
  `todo_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_todo_member` (`todo_id`,`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

