package br.com.beertech.fusion.service;

import br.com.beertech.fusion.domain.Operation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RabbitListener(queues = "${spring.rabbitmq.queues.operation}")
public class OperationListener implements MessageListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(OperationListener.class);

  public void onMessage(Message message) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String json = new String(message.getBody(), StandardCharsets.UTF_8);

      LOGGER.info("Receive Operation Message {}", json);

      Operation transactionPojo = objectMapper.readValue(json, Operation.class);

      if (new Validator(transactionPojo).validateResponseRMQ()) {
        new RestClient(transactionPojo).sendPostOperationAPI();
      }
    } catch (JsonProcessingException e) {
      LOGGER.error("Error Exception {}", e.getMessage());
    }
  }
}
