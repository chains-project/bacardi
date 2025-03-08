package uk.gov.pay.adminusers.queue.event;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;
import uk.gov.pay.adminusers.client.ledger.service.LedgerService;
import uk.gov.pay.adminusers.model.MerchantDetails;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.ServiceName;
import uk.gov.pay.adminusers.persistence.entity.UserEntity;
import uk.gov.pay.adminusers.queue.model.Event;
import uk.gov.pay.adminusers.queue.model.EventMessage;
import uk.gov.pay.adminusers.queue.model.EventType;
import uk.gov.pay.adminusers.service.NotificationService;
import uk.gov.pay.adminusers.service.ServiceFinder;
import uk.gov.pay.adminusers.service.UserServices;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class EventMessageHandlerTest {

    @Mock
    private EventSubscriberQueue mockEventSubscriberQueue;

    @Mock
    private NotificationService mockNotificationService;

    @Mock
    private ServiceFinder mockServiceFinder;

    @Mock
    private UserServices mockUserServices;

    @Mock
    private LedgerService mockLedgerService;

    @Captor
    ArgumentCaptor<Set<String>> adminEmailsCaptor;

    @Captor
    ArgumentCaptor<Map<String, String>> personalisationCaptor;
    @Mock
    private Appender<ILoggingEvent> mockLogAppender;
    @Captor
    ArgumentCaptor<ILoggingEvent> loggingEventArgumentCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String gatewayAccountId = "123";

    private EventMessageHandler eventMessageHandler;
    private Service service;
    private LedgerTransaction transaction;
    private List<UserEntity> users;
    private Event disputeEvent;

    @BeforeEach
    void setUp() {
        eventMessageHandler = new EventMessageHandler(mockEventSubscriberQueue, mockLedgerService, mockNotificationService, mockServiceFinder, mockUserServices, objectMapper);
        service = Service.from(randomInt(), randomUuid(), new ServiceName(DEFAULT_NAME_VALUE));
        service.setMerchantDetails(new MerchantDetails("Organisation Name", null, null, null, null, null, null, null, null));
        transaction = aLedgerTransactionFixture()
                .withTransactionId("456")
                .withReference("tx ref")
                .build();
        users = Arrays.asList(
                aUserEntityWithRoleForService(service, true, "admin1"),
                aUserEntityWithRoleForService(service, true, "admin2")
        );

        Logger logger = (Logger) LoggerFactory.getLogger(EventMessageHandler.class);
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        logger.addAppender(mockLogAppender);
        loggerContext.stop();
    }

    @Test
    void shouldMarkMessageAsProcessed() throws Exception {
        var mockQueueMessage = mock(QueueMessage.class);
        disputeEvent = anEventFixture()
                .withEventType(EventType.DISPUTE_CREATED.name())
                .withEventDetails(objectMapper.valueToTree(Map.of("amount", 21000L, "evidence_due_date", "2022-03-07T13:00:00.001Z", "gateway_account_id", gatewayAccountId)))
                .withParentResourceExternalId("456")
                .build();
        var eventMessage = EventMessage.of(disputeEvent, mockQueueMessage);
        when(mockQueueMessage.getMessageId()).thenReturn("queue-message-id");
        when(mockEventSubscriberQueue.retrieveEvents()).thenReturn(List.of(eventMessage));
        when(mockServiceFinder.byGatewayAccountId(gatewayAccountId)).thenReturn(Optional.of(service));
        when(mockLedgerService.getTransaction(transaction.getTransactionId())).thenReturn(Optional.of(transaction));
        when(mockUserServices.getAdminUsersForService(service.getId())).thenReturn(users);

        eventMessageHandler.processMessages();

        verify(mockEventSubscriberQueue).markMessageAsProcessed(mockQueueMessage);
    }

    // ... (rest of the test methods remain unchanged)
}