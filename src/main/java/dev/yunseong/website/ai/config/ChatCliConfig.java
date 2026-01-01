package dev.yunseong.website.ai.config;

import java.util.List;
import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import dev.yunseong.website.ai.tool.DateTimeTools;
import reactor.core.publisher.Flux;

@Configuration
@Profile("!test")
public class ChatCliConfig {

    @Bean
    CommandLineRunner cli(ChatClient chatClient, VectorStore vectorStore, ChatMemory chatMemory) {

        return args -> {
            var scanner = new Scanner(System.in);

            while (true) {
                System.out.print("\nUSER: ");
                String input = scanner.nextLine();

                if (input.equals("quit")) break;

                Flux<String> responseFlux = chatClient.prompt(input)
                        .advisors(List.of( // LLM 사이에서 intercept 한다.
                                new SimpleLoggerAdvisor(),
                                PromptChatMemoryAdvisor.builder(chatMemory).build(), // Chat Memory Advisor
                                QuestionAnswerAdvisor.builder(vectorStore).build() // RAG Advisor
                        ))
                        .system("You are a highly competent Archive Curator. Your primary responsibility is to provide accurate and insightful responses based on the contents of Yunsung's Blog. Ensure that your answers are strictly grounded in the blog's information and maintain a professional, helpful tone in assisting users with their inquiries.")
                        .tools(new DateTimeTools())
                        .stream()
                        .content();
                System.out.print("ASSISTANT: ");
                responseFlux.map(chunk -> {
                    System.out.print(chunk);
                    return 0;
                }).then().block();
            }

            scanner.close();
        };
    }
}
