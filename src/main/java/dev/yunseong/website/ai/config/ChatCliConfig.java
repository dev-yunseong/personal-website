package dev.yunseong.website.ai.config;

import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.yunseong.website.ai.domain.BlogAgent;
import dev.yunseong.website.global.config.ConditionalOnCuratorEnabled;
import reactor.core.publisher.Flux;

@ConditionalOnCuratorEnabled
@Configuration
//@Profile("!test")
public class ChatCliConfig {

    @Bean
    CommandLineRunner cli(BlogAgent blogAgent) {

        return args -> {
            var scanner = new Scanner(System.in);

            System.out.print("\nUSER: ");

            while (scanner.hasNextLine()) {
                try {
                    String input = scanner.nextLine();
                    if (input.equals("quit")) break;

                    Flux<String> responseFlux = blogAgent.prompt(input);
                    System.out.print("ASSISTANT: ");
                    responseFlux.map(chunk -> {
                        System.out.print(chunk);
                        return 0;
                    }).then().block();

                    System.out.print("\nUSER: ");
                } catch (Exception e) {
                    break;
                }
            }

            scanner.close();
        };
    }
}
