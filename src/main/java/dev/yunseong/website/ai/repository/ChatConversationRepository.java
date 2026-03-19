package dev.yunseong.website.ai.repository;

import dev.yunseong.website.ai.domain.ChatConversation;
import dev.yunseong.website.manage.domain.IpLatestChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Page<ChatConversation> findByIpOrderByCreatedAtDesc(String ip, Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.IpLatestChat(c.ip, MAX(c.createdAt)) " +
                   "FROM ChatConversation c GROUP BY c.ip ORDER BY MAX(c.createdAt) DESC",
           countQuery = "SELECT COUNT(DISTINCT c.ip) FROM ChatConversation c")
    Page<IpLatestChat> findDistinctIpsOrderedByLatest(Pageable pageable);
}
