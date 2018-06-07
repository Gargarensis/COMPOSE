import com.google.gson.Gson;
import com.intellij.ide.util.PropertyName;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import io.grpc.Server;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WindowFactory implements ToolWindowFactory {
    private JPanel myToolWindowContent;
    private JTextField txtQuery;
    private JPanel pnlResult;
    private ToolWindow myToolWindow;
    private ArrayList<SearchResult> currentResult = new ArrayList<>();
    private JScrollPane scrollPane;
    private JPanel contentPane;
    private SettingsState settings;
    private String currentQuery;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        myToolWindow = toolWindow;
        settings = SettingsState.getInstance();
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(myToolWindowContent, "", false);
        toolWindow.getContentManager().addContent(content);
        txtQuery.addKeyListener(txtListener);
    }

    public String executeQuery(String query) {
        try {
            StringBuilder result = new StringBuilder();

            URL url = new URL(settings.getCustomServer() + "/search?query=" + query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            return result.toString();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean postVote(long id, int action) {
        try {

            URL url = new URL(settings.getCustomServer() + "/vote");
            Map<String,Object> params = new LinkedHashMap<>();
            params.put("id", id);
            params.put("action", action);
            params.put("query", currentQuery);

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String,Object> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");

            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);
            //            conn.getOutputStream().flush();
            //            conn.getOutputStream().close();

            Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

            String res = "";

            for (int c; (c = in.read()) >= 0;)
                res += (char)c;

            Gson gson = new Gson();
            String errString;
            try {
                ServerError err = gson.fromJson(res, ServerError.class);
                errString = err.getError();
                if ("null".equals(errString)) {
                    return true;
                }
                return false;
            } catch (Exception e2) {
                System.out.println(e2.getMessage());
                return true;
            }
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }

    public void search(String str) {

        // remove problematic sql/route characters
        str = str.replace("\\", "");
        str = str.replace("\'", "");
        str = str.replace("\"", "");
        str = str.replace("%", "");
        str = str.replace("`", "");

        if (str == null || str == "")
            return;

        currentQuery = str;

        String fullQuery;
        try {
            fullQuery = URLEncoder.encode(str, "UTF-8");
            fullQuery += "&accuracy=" +  URLEncoder.encode(Double.toString(settings.getAccuracy()), "UTF-8");
            fullQuery += "&max=" + URLEncoder.encode(Integer.toString(settings.getMaxResults()), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            fullQuery = null;
        }

        currentResult.clear();

        if (fullQuery == null || fullQuery == "")
            return;

        String data = executeQuery(fullQuery);

        if (data == "" || data == null) {
            JLabel errLbl = new JLabel("<html><center>Server unavailable.<br>Check the settings if you are using a custom server.</center></html>");
            contentPane.removeAll();
            contentPane.invalidate();

            contentPane.add(errLbl);

            contentPane.getParent().revalidate();
            contentPane.getParent().repaint();
            return;
        }

        SearchResult[] result = {};

        Gson gson = new Gson();
        try {
            result = gson.fromJson(data, SearchResult[].class);
        } catch (Exception e) {
            String errString;
            try {
                ServerError err = gson.fromJson(data, ServerError.class);
                errString = err.getError();
            } catch (Exception e2) {
                errString = e2.getMessage();
            }

            JLabel errLbl = new JLabel(errString);
            contentPane.removeAll();
            contentPane.invalidate();

            contentPane.add(errLbl);

            contentPane.getParent().revalidate();
            contentPane.getParent().repaint();

        }

        if (result.length <= 0) {
            return;
        }

        for (int i = 0; i < result.length; i++) {
            currentResult.add(result[i]);
        }

        renderResults();

        myToolWindowContent.revalidate();
        myToolWindowContent.repaint();
        myToolWindowContent.updateUI();

    }

    public JButton createIconButton(String iconPath, String toolTip) {
        JButton result = new JButton();
        try {
            Image img = ImageIO.read(getClass().getResource(iconPath));
            result.setIcon(new ImageIcon(img));
            result.setBorder(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        result.setToolTipText(toolTip);
        result.setPreferredSize(new Dimension(16,16));
        result.setMaximumSize(new Dimension(16,16));
        result.setMinimumSize(new Dimension(16,16));
        result.setSize(new Dimension(16,16));

        return result;
    }

    public ActionListener generateUpDownListener (String iconPath, JButton other, long id) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(e.getSource() instanceof JButton){
                    JButton source = (JButton) e.getSource();
                    if (source.getName().contains("selected")) {
                        return;
                    }
                    boolean canUpdate;

                    String otherPath = "";
                    if (other.getName() == "up") {
                        canUpdate = postVote(id, -1);
                        if (canUpdate) {
                            otherPath = "icons/up.png";
                            settings.addDownVotedId(currentQuery, id);
                            source.setName("down_selected");
                        }
                    } else if (other.getName() == "down") {
                        canUpdate = postVote(id, 1);
                        if (canUpdate) {
                            otherPath = "icons/down.png";
                            settings.addUpVotedId(currentQuery, id);
                            source.setName("up_selected");
                        }
                    } else if (other.getName() == "up_selected") {
                        canUpdate = postVote(id, -2);
                        if (canUpdate) {
                            otherPath = "icons/up.png";
                            settings.addDownVotedId(currentQuery, id);
                            settings.removeUpvote(currentQuery, id);
                            source.setName("down_selected");
                            other.setName("up");
                        }
                    } else if (other.getName() == "down_selected") {
                        canUpdate = postVote(id, 2);
                        if (canUpdate) {
                            otherPath = "icons/down.png";
                            settings.addUpVotedId(currentQuery, id);
                            settings.removeDownvote(currentQuery, id);
                            source.setName("up_selected");
                            other.setName("down");
                        }
                    } else {
                        return;
                    }

                    if (!canUpdate) {
                        return;
                    }

                    try {
                        Image otherImg = ImageIO.read(getClass().getResource(otherPath));
                        other.setIcon(new ImageIcon(otherImg));
                        Image img = ImageIO.read(getClass().getResource(iconPath));
                        source.setIcon(new ImageIcon(img));
                    } catch (IOException err) {
                        err.printStackTrace();
                    }
                }
            }
        };
    }

    public String getAccuracyColor(double power)
    {
        double H = power * 0.4; // Hue (note 0.4 = Green, see huge chart below)
        double S = 0.9; // Saturation
        double B = 0.9; // Brightness

        Color aC = Color.getHSBColor((float)H, (float)S, (float)B);

        String r = Integer.toHexString(aC.getRed());
        String g = Integer.toHexString(aC.getGreen());
        String b =  Integer.toHexString(aC.getBlue());

        return "#" + r + g + b;
    }

    public JPanel buildPopup(SearchResult sR) {
        JPanel mainPnl = new JPanel();
        mainPnl.setLayout(new BorderLayout());


        String titleString = String.format("<html><div width=%d>%s</div></html>", 800, sR.getMessage());

        JPanel bottomContainer = new JPanel(new BorderLayout());
        JLabel accuracy = new JLabel("<html>Similarity Threshold: " + sR.getScore() + " <span style=\"color:" + getAccuracyColor(sR.getScore()) + "\"> &#x25cf;</span></html>");
        accuracy.setToolTipText("Quantify the similarity of the code and the query. Usually goes from 0 to 1, but it can be less or more based on user's votes.");
        JLabel external;
        if (sR.getName() == null || sR.getName() == ""){
            external = new JLabel("<html><a>Show the source</a></html>");
        } else {
            external =  new JLabel("<html><a>View " + sR.getName() + " repository on GitHub</a></html>");
        }
        external.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Font font = external.getFont();
        Map attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        external.setFont(font.deriveFont(attributes));
        external.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        String url = sR.getUrl();
                        if (sR.getHash() != null && sR.getHash() != "") {
                            if (url.endsWith(".git")) {
                                url = url.substring(0, url.length() - 4);
                            }
                            url += "/commit/" + sR.getHash();
                        }
                        URI uri = new URI(url);
                        desktop.browse(uri);
                    } catch (IOException ex) {
                        // do nothing
                    } catch (URISyntaxException ex) {
                        //do nothing
                    }
                } else {
                    //do nothing
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {
                external.setForeground(new Color(59,201,229));
                Font font = external.getFont();
                Map attributes = font.getAttributes();
                attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                external.setFont(font.deriveFont(attributes));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                external.setForeground(Color.black);
            }
        });

        bottomContainer.add(accuracy, BorderLayout.CENTER);
        bottomContainer.add(external, BorderLayout.EAST);

        bottomContainer.setBorder(BorderFactory.createCompoundBorder(
                bottomContainer.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 3, 5)));

        mainPnl.add(bottomContainer, BorderLayout.SOUTH);

        JLabel title = new JLabel(titleString);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        title.setBorder(BorderFactory.createCompoundBorder(
                title.getBorder(),
                BorderFactory.createEmptyBorder(10, 5, 5, 5)));
        mainPnl.add(title, BorderLayout.NORTH);

        JTextArea code = new JTextArea();
        code.setText(sR.getCode());
        code.setFont(code.getFont().deriveFont(12f));
        code.setBorder(BorderFactory.createCompoundBorder(
                code.getBorder(),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));

        JScrollPane scroll = new JScrollPane(code);
        scrollPane.getViewport().setBackground(Color.white);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainPnl.add(scroll, BorderLayout.CENTER);

        mainPnl.setMinimumSize(new Dimension(800, 800));
        mainPnl.setPreferredSize(new Dimension(800, 800));
        mainPnl.setMaximumSize(new Dimension(800, 800));

        return mainPnl;
    }

    public void renderResults() {

        contentPane.removeAll();
//        contentPane.revalidate();
//        contentPane.repaint();
        int gridy = -1;

        GridBagConstraints b = new GridBagConstraints();
        b.fill = GridBagConstraints.BOTH;
        b.weightx = 1;
        b.gridx = 0;
        b.anchor = GridBagConstraints.FIRST_LINE_START;
        b.insets = new Insets(0,3,10,3);

        for (SearchResult sR : currentResult) {

            HashMap<String, Long[]> downVotedMap = settings.getDownVoted();
            HashMap<String, Long[]> upVotedMap = settings.getUpVoted();
            boolean upVoted = false;

            if (ArrayUtils.contains(downVotedMap.get(currentQuery), sR.getId())) {
                // hide downvoted code
                continue;
            }

            if (ArrayUtils.contains(upVotedMap.get(currentQuery), sR.getId())) {
                upVoted = true;
            }

            JPanel resultContainer = new JPanel();
            resultContainer.setLayout(new GridBagLayout());

            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.weighty = 0;
            c.ipady = 10;

            JPanel titleContainer = new JPanel();
            titleContainer.setLayout(new BorderLayout());
            titleContainer.setBorder(BorderFactory.createCompoundBorder(
                    titleContainer.getBorder(),
                    BorderFactory.createEmptyBorder(0, 20, 0, 10)));
            // TODO: double click on title to add code?
            JLabel title = new JLabel();
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            title.setText(sR.getMessage());
            title.setMinimumSize(new Dimension(75, 14));
            title.setPreferredSize(new Dimension(75, 14));
            title.setMaximumSize(new Dimension(75, 14));
            JPanel titleButtons = new JPanel();

            JButton expand = createIconButton("icons/ic_fullscreen_black_24dp_1x.png", "Expand code...");
            expand.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JComponent mainPnl = buildPopup(sR);

                    ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(mainPnl, mainPnl);
                    JBPopup popup = popupBuilder.createPopup();
                    popup.showInFocusCenter();
                }
            });
            titleButtons.add(expand, BorderLayout.EAST);

            JButton down = createIconButton("icons/down.png", "Not relevant at all");;
            JButton up = createIconButton("icons/up.png", "Useful code!");

            down.setName("down");
            up.setName("up");

            // set green arrow icon if the code was upvoted previously
            if (upVoted) {
                up = createIconButton("icons/up_selected.png", "Useful code!");
                up.setName("up_selected");
            }

            down.addActionListener(generateUpDownListener("icons/down_selected.png", up, sR.getId()));
            up.addActionListener(generateUpDownListener("icons/up_selected.png", down, sR.getId()));

            titleButtons.add(down, BorderLayout.EAST);
            titleButtons.add(up, BorderLayout.EAST);

            JPanel titleButtonsPnl = new JPanel();
            titleButtonsPnl.setLayout(new BorderLayout());
            titleButtonsPnl.add(titleButtons, BorderLayout.SOUTH);

            titleContainer.add(titleButtonsPnl, BorderLayout.EAST);
            titleContainer.add(title, BorderLayout.CENTER);

            JTextArea codeArea = new JTextArea();

            // todo: set selectable but not editable?
            codeArea.setText(sR.getPreviewCode(10)); // code length: 10 lines
            codeArea.setFont(codeArea.getFont().deriveFont(10f));
            codeArea.setBorder(BorderFactory.createCompoundBorder(
                    codeArea.getBorder(),
                    BorderFactory.createEmptyBorder(5, 10, 5, 15)));
            codeArea.setMaximumSize(new Dimension(-1, 25));
            codeArea.setPreferredSize(new Dimension(-1, 25));
            codeArea.setMinimumSize(new Dimension(-1, 25));
            codeArea.setSize(new Dimension(-1, 25));

            JScrollPane scrollPane = new PDControlScrollPane(codeArea);
            JPanel rows = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

            String nstr = "";

            for (int i = 1; i < 10; i++) {
                nstr = nstr + i + "<br>";
            }

            nstr = nstr + "…";

            JLabel numbers = new JLabel("<html>" + nstr + "</html>");
            numbers.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            numbers.setHorizontalAlignment(SwingConstants.RIGHT);
            numbers.setBorder(BorderFactory.createCompoundBorder(
                    numbers.getBorder(),
                    BorderFactory.createEmptyBorder(5, 10, 5, 5)));
            rows.add(numbers);

            scrollPane.setRowHeaderView(rows);
            scrollPane.getViewport().setBackground(Color.white);

            resultContainer.add(titleContainer, c);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());

            c.ipady = 115;
            c.gridy = 1;
            c.weightx = 1;
            c.weighty = 0;
            c.gridwidth = 1;

            resultContainer.add(scrollPane, c);

            b.gridy = gridy++;

            contentPane.add(resultContainer, b);
        }
        contentPane.revalidate();
        contentPane.repaint();
    }

    KeyAdapter txtListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                search(txtQuery.getText());
            }
        }
    };

    @Override
    public void init(ToolWindow window) {

    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return false;
    }

    @Override
    public boolean isDoNotActivateOnStart() {
        return false;
    }

    private void createUIComponents() {
        pnlResult = new JPanel();

        contentPane = new JPanel();
        //        contentPane.setBorder(BorderFactory.createLineBorder(Color.red));
        contentPane.setLayout(new GridBagLayout());

        scrollPane = new JScrollPane(contentPane);

        pnlResult.setLayout(new BorderLayout());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        pnlResult.setBorder(BorderFactory.createEmptyBorder());
        pnlResult.add(scrollPane, BorderLayout.CENTER);
        pnlResult.setVisible(true);
    }
}
