package br.com.microservices.orchestrated.orderservice.core.repository;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {

    List<Event> findAllByOrderByCreatedAtDesc();

    Optional<Event> findTop1ByTransactionIdOrderByCreatedAt(String transactionId);

    Optional<Event> findTop1ByOrderIdOrderByCreatedAt(String orderId);

}
