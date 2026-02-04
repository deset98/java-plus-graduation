package ru.practicum.ewm.client;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class GrpcAnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub stub;

    public Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {

        try {
            Iterator<RecommendedEventProto> iterator = stub.getRecommendationsForUser(request);
            return asStream(iterator);
        } catch (Exception e) {
            log.error("Ошибка при получении рекомендованных Event: {}", e.getMessage());
        }

        return Stream.empty();
    }

    public Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {

        try {
            Iterator<RecommendedEventProto> iterator = stub.getSimilarEvents(request);
            return asStream(iterator);
        } catch (Exception e) {
            log.error("Ошибка при получении похожих Event: {}", e.getMessage());
        }

        return Stream.empty();

    }

    public Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {

        try {
            Iterator<RecommendedEventProto> iterator = stub.getInteractionsCount(request);
            return asStream(iterator);
        } catch (Exception e) {
            log.error("Ошибка при вычислении Interaction: {}", e.getMessage());
        }

        return Stream.empty();
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false);
    }
}