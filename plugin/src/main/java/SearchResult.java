public class SearchResult {

    Long id;
    String code;
    String comments;
    String url;
    String name;
    String hash;
    String message;
    Double score;

    // getter and setter

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getPreviewCode(int n) {

        String[] lines = code.split("\n");

        if (lines.length <= 10) {
            return code;
        }
        else {
            String result = lines[0];
            for (int i = 1; i < n; i++) {
                result = result + "\n" + lines[i];
            }
            return result;
        }
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public SearchResult(String json) {
        // parse json data
    }

    public SearchResult() {
        // nothing atm
    }
}
