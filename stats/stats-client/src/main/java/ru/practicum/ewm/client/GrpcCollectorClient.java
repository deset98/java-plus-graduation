package ru.practicum.ewm.client;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Slf4j
@Component
public class GrpcCollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub stub;

    public void collectUserAction(UserActionProto request) {
        try {
            stub.collectUserAction(request);
        } catch (Exception e) {
            log.error("Ошибка получения действий пользователя: {}", e.getMessage());
        }
    }
}