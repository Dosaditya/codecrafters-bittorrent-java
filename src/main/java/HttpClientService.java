import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HttpClientService {
    private final HttpClient client;

    public HttpClientService() {
        this.client = HttpClient.newHttpClient();
    }

    public RequestURLBuilder newRequestURLBuilder(String baseURL) {
        return new RequestURLBuilder(baseURL);
    }

    public HttpResponse<byte[]> sendGetRequest(String url) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    public static class RequestURLBuilder {
        private final StringBuilder url;
        private boolean hasParams;

        public RequestURLBuilder(String baseURL) {
            this.url = new StringBuilder(baseURL);
            this.hasParams = false;
        }

        public RequestURLBuilder addParams(List<Pair<String, String>> params) {
            for (Pair<String, String> param : params) {
                addParam(param.getLeft(), param.getRight());
            }
            return this;
        }

        public RequestURLBuilder addParam(String key, String value) {
            if (!hasParams) {
                url.append("?");
                hasParams = true;
            } else {
                url.append("&");
            }
            url.append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.ISO_8859_1));
            return this;
        }

        public String build() {
            return url.toString();
        }
    }
}