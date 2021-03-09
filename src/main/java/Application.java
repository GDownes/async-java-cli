import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

class Application {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        var httpClient = HttpClient.newHttpClient();
        var tendersRequest = HttpRequest.newBuilder().uri(URI.create("https://api.tender-ni.com/tender")).build();
        var tendersResponse = httpClient.send(tendersRequest, HttpResponse.BodyHandlers.ofString());
        var tenders = mapper.readValue(tendersResponse.body(), new TypeReference<List<Map<String, Object>>>() {});
        var tenderDescriptions = tenders.stream()
                .map(tender -> getTenderDescription(tender.get("id").toString()))
                .collect(Collectors.toList());
        CompletableFuture.allOf(tenderDescriptions.toArray(new CompletableFuture[0]))
                .thenApply(v -> tenderDescriptions.stream().map(CompletableFuture::join).collect(Collectors.toList()))
                .thenAccept(System.out::println).get();
    }

    private static CompletableFuture<String> getTenderDescription(String id) {
        return CompletableFuture.supplyAsync(() -> {
            var httpClient = HttpClient.newHttpClient();
            var tenderRequest = HttpRequest.newBuilder().uri(URI.create("https://api.tender-ni.com/tender/" + id)).build();
            return httpClient.sendAsync(tenderRequest, HttpResponse.BodyHandlers.ofString());
        }).thenApply(tenderResponse -> {
            try {
                var tender = mapper.readValue(tenderResponse.get().body(), new TypeReference<Map<String, Object>>() {});
                return tender.get("description").toString();
            } catch (Exception e) {
                return "Unknown";
            }
        });
    }
}