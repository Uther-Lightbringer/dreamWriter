package org.zenithon.articlecollect;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
public class ArticleCollectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticleCollectApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(Environment env) {
        return args -> {
            String port = env.getProperty("server.port", "8080");
            String contextPath = env.getProperty("server.servlet.context-path", "");
            String hostAddress = getLocalHostAddress();
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("✅ 项目启动成功！");
            System.out.println("=".repeat(60));
            System.out.println("🌐 本地访问地址：http://localhost:" + port + contextPath);
            System.out.println("🌐 网络访问地址：http://" + hostAddress + ":" + port + contextPath);
            System.out.println("📊 H2 控制台地址：http://localhost:" + port + contextPath + "/h2-console");
            System.out.println("=".repeat(60) + "\n");
        };
    }

    private String getLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

}
