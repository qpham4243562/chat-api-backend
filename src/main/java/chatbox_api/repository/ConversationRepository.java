package chatbox_api.repository;


import chatbox_api.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {
}
