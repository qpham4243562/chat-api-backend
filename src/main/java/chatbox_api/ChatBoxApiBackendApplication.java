package chatbox_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;



@SpringBootApplication
public class ChatBoxApiBackendApplication {

	public static void main(String[] args) {
		// Tải biến môi trường từ file .env
		Dotenv dotenv = Dotenv.configure()
				.directory("./") // Đường dẫn đến thư mục chứa file .env
				.ignoreIfMalformed() // Bỏ qua lỗi định dạng
				.ignoreIfMissing() // Bỏ qua nếu thiếu file
				.load();

		SpringApplication.run(ChatBoxApiBackendApplication.class, args);
	}
}
