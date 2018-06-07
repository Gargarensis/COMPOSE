import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.apache.commons.lang.ArrayUtils;

import javax.annotation.Nullable;
import java.util.HashMap;

@State(
        name="SettingsState",
        storages = {
                @Storage("ComposeSettings.xml")}
)
class SettingsState implements PersistentStateComponent<SettingsState> {

    public double accuracy = 0.4f;
    public int maxResults = 100;
    public HashMap<String, Long[]> downVoted = new HashMap<>();
    public HashMap<String, Long[]> upVoted = new HashMap<>();
    public String customServer = "http://localhost:12345/servlet";

    public SettingsState() { }

    @Nullable
    @Override
    public SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Nullable
    public static SettingsState getInstance() {
        return ServiceManager.getService(SettingsState.class);
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public void addDownVotedId(String query, long id) {
        if (downVoted.containsKey(query)) {
            Long[] temp = (Long[]) ArrayUtils.add(downVoted.get(query), id);
            downVoted.put(query, temp);
        }
        else {
            Long[] temp = {id};
            downVoted.put(query, temp);
        }
    }

    public void addUpVotedId(String query, long id) {
        if (upVoted.containsKey(query)) {
            Long[] temp = (Long[]) ArrayUtils.add(upVoted.get(query), id);
            upVoted.put(query, temp);
        }
        else {
            Long[] temp = {id};
            upVoted.put(query, temp);
        }
    }

    public void removeUpvote(String query, long id) {
        Long[] upvoted = upVoted.get(query);
        if (upvoted == null) {
            return;
        }
        upvoted = (Long[]) ArrayUtils.remove(upvoted, ArrayUtils.indexOf(upvoted, id));
        upVoted.put(query, upvoted);
    }

    public void removeDownvote(String query, long id) {
        Long[] downvoted = upVoted.get(query);
        if (downvoted == null) {
            return;
        }
        downvoted = (Long[]) ArrayUtils.remove(downvoted, ArrayUtils.indexOf(downvoted, id));
        downVoted.put(query, downvoted);
    }

    public HashMap<String, Long[]> getDownVoted() {
        return downVoted;
    }

    public HashMap<String, Long[]> getUpVoted() {
        return upVoted;
    }

    public String getCustomServer() {
        return customServer;
    }

    public void setCustomServer(String customServer) {
        this.customServer = customServer;
    }
}
