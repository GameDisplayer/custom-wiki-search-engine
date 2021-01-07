import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to create an user interface
 */
public class Application extends JFrame {

    /* Top menu */
    private JMenuBar menuBar = new JMenuBar();
    private JMenu menu = new JMenu("File");
    private JMenu menu2 = new JMenu("Statistics");

    private JMenuItem item1 = new JMenuItem("Export");
    private JMenuItem item2 = new JMenuItem("Close");

    private JMenuItem item3 = new JMenuItem("General");
    private JMenuItem item4 = new JMenuItem("Topics");


    private final Main main = new Main();
    private final JTextField jtf = new JTextField("Search the Wiki world");
    private final JCheckBox numFields = new JCheckBox("Multiple field", false);
    private int numF = 1;
    private final Object[] fields = new Object[]{"title", "abstract", "content"};
    private final JComboBox field1 = new JComboBox(fields);
    private final JComboBox field2 = new JComboBox(fields);
    private final JComboBox field3 = new JComboBox(fields);
    //These two JLabels are defined here because we need to access it, in order to make them visibles.
    private final JLabel content2 = new JLabel(";", JLabel.LEFT);
    private final JLabel content3 = new JLabel(";", JLabel.LEFT);
    ImageIcon helpIcon = createImageIcon("Icon/help.png", "help");
    JButton helpButton = new JButton(helpIcon);
    ImageIcon returnIcon = createImageIcon("Icon/back.png", "back");
    JButton returnButton = new JButton(returnIcon);
    ImageIcon contentIcon = createImageIcon("Icon/content.png", "content");
    JButton contentButton = new JButton(contentIcon);
    JList list = new JList();
    JScrollPane scrollableList = new JScrollPane(list);
    private List<Document> actualResult = new ArrayList<>();
    private int selectedResult = 0;
    boolean seeingText = false;
    private final Color backgroundColor = new Color(250, 240, 230);
    private final Color buttonColor = new Color(222, 184, 135);
    private JCheckBox historyTopic = new JCheckBox("History");
    private JCheckBox religionTopic = new JCheckBox("Religion");
    private JCheckBox sciencesTopic = new JCheckBox("Sciences");
    private ScoreDoc[] actualScore;

    private JCheckBox synonyms = new JCheckBox("Synonyms", false);

    List<Document> docsHistory, docsScience, docsReligion;

    /**
     * Constructor for the application
     */
    public Application() throws IOException {

        //Load the documents per topic for specific searches
        TopicModeling tm = new TopicModeling();
        this.docsHistory = tm.docsHistory;
        this.docsReligion = tm.docsReligion;
        this.docsScience = tm.docsScience;

        this.setTitle("Wiki Search");
        this.setSize(750, 660);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        //initialize menu
        this.menu.add(item1);
        this.menu.addSeparator();
        item2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        this.menu.add(item2);
        this.menu2.add(item3);
        this.menu2.add(item4);

        this.menuBar.add(menu);
        this.menuBar.add(menu2);
        this.setJMenuBar(menuBar);

        JPanel container = new JPanel();
        container.setBackground(backgroundColor);
        container.setBorder(new EmptyBorder(0,10,10,10));
        container.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setBackground(backgroundColor);
        JPanel sidesButton = new JPanel();
        sidesButton.setLayout(new BoxLayout(sidesButton, BoxLayout.Y_AXIS));
        sidesButton.setBackground(backgroundColor);
        JPanel med = new JPanel();
        med.setLayout(new BoxLayout(med, BoxLayout.X_AXIS));
        med.setBackground(backgroundColor);
        //Panel just for the string "Filters:"
        JPanel text = new JPanel();
        text.setBackground(backgroundColor);
        //Panel for the checkBox
        JPanel multFields = new JPanel();
        multFields.setLayout(new BoxLayout(multFields, BoxLayout.LINE_AXIS));
        multFields.setBackground(backgroundColor);
        //Panel for choosing the field(s)
        JPanel topics = new JPanel();
        topics.setLayout(new BoxLayout(topics, BoxLayout.LINE_AXIS));
        topics.setBackground(backgroundColor);
        //Panel for all the filters
        JPanel choosable = new JPanel();
        choosable.setLayout(new BoxLayout(choosable, BoxLayout.Y_AXIS));
        choosable.setBackground(backgroundColor);
        //Panel for the synonyms
        JPanel synon = new JPanel();
        synon.setLayout(new BoxLayout(synon, BoxLayout.LINE_AXIS));
        synon.setBackground(backgroundColor);
        //Panel with all the filters (need to align all)
        JPanel filters = new JPanel(new BorderLayout());
        filters.setBackground(backgroundColor);

        //Search bar :
        Font police = new Font("Arial", Font.BOLD, 14);
        jtf.setFont(police);
        jtf.setPreferredSize(new Dimension(650, 30));
        jtf.setForeground(Color.GRAY);
        jtf.setBorder(new LineBorder(Color.BLACK));
        top.add(jtf);
        jtf.addKeyListener(new JtextFileEnterListener(this));
        jtf.addMouseListener(new MouseAdapter(){
            int numClick = 0;
            @Override
            public void mouseClicked(MouseEvent e){
                numClick += 1;
                if(numClick == 1) {
                    jtf.setText("");
                    jtf.setForeground(Color.BLACK);
                }
                selectedResult = 0;
                seeingText = false;
            }
        });

        //Go button :
        JButton jb = new JButton("GO");
        jb.setBackground(buttonColor);
        jb.setPreferredSize(new Dimension(55,30));
        jb.setBorder(new LineBorder(Color.BLACK));
        jb.addActionListener(new ButtonListener(this));
        top.add(jb);

        //Result part :
        list.setFont(police);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new getAbstract(this));
        scrollableList.setPreferredSize(new Dimension(700, 500));
        scrollableList.setMaximumSize(new Dimension(700, 500));
        scrollableList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollableList.setAlignmentY(Component.TOP_ALIGNMENT);
        scrollableList.setAlignmentX(Component.CENTER_ALIGNMENT);
        scrollableList.setBorder(new LineBorder(Color.BLACK));
        returnButton.setPreferredSize(new Dimension(32,20));
        returnButton.setMaximumSize(new Dimension(32,20));
        returnButton.addActionListener(new returnListener(this));
        returnButton.setIcon(null);
        returnButton.setOpaque(false);
        returnButton.setContentAreaFilled(false);
        returnButton.setBorderPainted(false);
        returnButton.setEnabled(false);
        returnButton.setBackground(buttonColor);
        returnButton.setBorder(new LineBorder(Color.BLACK));
        returnButton.setToolTipText("Back");
        sidesButton.add(returnButton);
        contentButton.setPreferredSize(new Dimension(32,32));
        contentButton.setMaximumSize(new Dimension(32,32));
        contentButton.addActionListener(new ContentListener(this));
        contentButton.setIcon(null);
        contentButton.setOpaque(false);
        contentButton.setContentAreaFilled(false);
        contentButton.setBorderPainted(false);
        contentButton.setEnabled(false);
        contentButton.setBackground(buttonColor);
        contentButton.setAlignmentY(Component.TOP_ALIGNMENT);
        contentButton.setBorder(new LineBorder(Color.BLACK));
        contentButton.setToolTipText("See the article content");
        sidesButton.add(contentButton);
        sidesButton.setAlignmentY(Component.TOP_ALIGNMENT);
        med.add(sidesButton);
        med.add(scrollableList);

        //String "Filters:" :
        JLabel filterString = new JLabel("Filters :", JLabel.RIGHT);
        filterString.setFont(police);
        text.add(filterString);

        //Selection of the fields :
        JLabel content = new JLabel("Field :", JLabel.LEFT);
        content.setFont(new Font("Arial", Font.ITALIC, 14));
        field1.setPreferredSize(new Dimension(80, 20));
        field1.setSelectedIndex(1);
        field1.setMaximumSize(new Dimension(80, 20));
        field1.setBackground(backgroundColor);
        content2.setFont(new Font("Arial", Font.ITALIC, 14));
        content2.setVisible(false);
        field2.setPreferredSize(new Dimension(80, 20));
        field2.setSelectedIndex(0);
        field2.setMaximumSize(new Dimension(80, 20));
        field2.setBackground(backgroundColor);
        field2.setVisible(false);
        content3.setFont(new Font("Arial", Font.ITALIC, 14));
        content3.setVisible(false);
        field3.setPreferredSize(new Dimension(80, 20));
        field3.setSelectedIndex(2);
        field3.setMaximumSize(new Dimension(80, 20));
        field3.setBackground(backgroundColor);
        field3.setVisible(false);

        //CheckBox for multipel fields :
        numFields.addItemListener(new MultiFieldsListener());
        numFields.addActionListener(new numberOfFields(this));
        numFields.setBackground(backgroundColor);
        helpButton.setPreferredSize(new Dimension(20,20));
        helpButton.addActionListener(new PopUpInformation(this));
        helpButton.setToolTipText("Help for the multiple fields search");
        helpButton.setBackground(buttonColor);
        helpButton.setBorder(new LineBorder(Color.BLACK));

        //Filter regarding the fields :
        multFields.add(helpButton);
        multFields.add(Box.createHorizontalStrut(10));
        multFields.add(content);
        multFields.add(Box.createHorizontalStrut(5));
        multFields.add(numFields);
        multFields.add(Box.createHorizontalStrut(5));
        multFields.add(field1);
        multFields.add(Box.createHorizontalStrut(5));
        multFields.add(content2);
        multFields.add(Box.createHorizontalStrut(5));
        multFields.add(field2);
        multFields.add(Box.createHorizontalStrut(5));
        multFields.add(content3);
        multFields.add(Box.createHorizontalStrut(5));
        multFields.add(field3);

        //Topics filters :
        JLabel topic = new JLabel("Topic :", JLabel.LEFT);
        topic.setFont(new Font("Arial", Font.ITALIC, 14));
        historyTopic.setBackground(backgroundColor);
        religionTopic.setBackground(backgroundColor);
        sciencesTopic.setBackground(backgroundColor);
        topics.add(topic);
        topics.add(Box.createHorizontalStrut(5));
        topics.add(historyTopic);
        topics.add(Box.createHorizontalStrut(5));
        topics.add(religionTopic);
        topics.add(Box.createHorizontalStrut(5));
        topics.add(sciencesTopic);

        //Allow search by synonyms
        JLabel txt = new JLabel("Allow :");
        txt.setFont(new Font("Arial", Font.ITALIC, 14));
        synonyms.setBackground(backgroundColor);
        synon.add(txt);
        synon.add(Box.createHorizontalStrut(5));
        synon.add(synonyms);

        //All the filters :
        multFields.setAlignmentX(Component.LEFT_ALIGNMENT);
        topics.setAlignmentX(Component.LEFT_ALIGNMENT);
        choosable.add(multFields);
        choosable.add(topics);
        filters.add(choosable, BorderLayout.WEST);
        filters.add(synon, BorderLayout.EAST);

        //General layout :
        container.add(top, BorderLayout.NORTH);
        container.add(med, BorderLayout.SOUTH);
        container.add(text, BorderLayout.WEST);
        container.add(filters, BorderLayout.CENTER);

        this.setContentPane(container);
        this.setVisible(true);
    }


    /**
     * Listener for "Go" button
     */
    public class ButtonListener implements ActionListener {
        JFrame current;

        /**
         * Constructor for the button "Go" listener
         * @param frame the current frame
         */
        public ButtonListener(JFrame frame){
            current = frame;
        }

        /**
         * Function allowing the search when clicking on the button
         * @param e the click
         */
        public void actionPerformed(ActionEvent e) {
            launchSearch(current);
        }
    }

    /**
     * Listener for the search bar
     */
    public class JtextFileEnterListener implements KeyListener {
        private final JFrame current;

        /**
         * Constructor for the enter listener
         * @param frame the current frame
         */
        public JtextFileEnterListener(JFrame frame){
            current = frame;
        }

        @Override
        public void keyTyped(KeyEvent e) {}

        /**
         * Function allowing the search when "Enter" is pressed
         * @param e the key pressed
         */
        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_ENTER){
                launchSearch(current);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {}
    }

    /**
     * Listener for the checkButton
     */
    public class MultiFieldsListener implements ItemListener{
        /**
         * Function allowing the activation of multifields search
         * @param e the state of the check button
         */
        @Override
        public void itemStateChanged(ItemEvent e) {
            if(e.getStateChange() == ItemEvent.DESELECTED){
                content2.setVisible(false);
                field2.setVisible(false);
                content3.setVisible(false);
                field3.setVisible(false);
                numF = 1;
            }
        }
    }

    /**
     * Action Listener for the check box
     */
    public class numberOfFields implements ActionListener{
        private final JFrame current;

        /**
         * Constructor for the Action Listener
         * @param frame the current frame
         */
        public numberOfFields(JFrame frame){
            current = frame;
        }

        /**
         * Method to allows the choose of the number of fields wanted
         * @param e
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if(numFields.isSelected()){
                Object[] possibilities = {2, 3};
                Object numOb = JOptionPane.showInputDialog(current,
                      "Select the number of fields wanted :",
                      "Number of fields",
                      JOptionPane.PLAIN_MESSAGE, helpIcon,
                      possibilities, 2);

                if(numOb != null){
                    numF = (int)numOb;
                }else{
                    numFields.setSelected(false);
                    numF = 1;
                }
                if(numF > 1){
                    content2.setVisible(true);
                    field2.setVisible(true);
                    if(numF == 3){
                        content3.setVisible(true);
                        field3.setVisible(true);
                    }
                }
            }
        }
    }

    /**
     * ActionListener for the help button
     */
    public static class PopUpInformation implements ActionListener{
        private final JFrame current;

        /**
         * Constructor for the ActionListener
         * @param frame the current frame
         */
        public PopUpInformation(JFrame frame){
            current = frame;
        }

        /**
         * Function displaying the information for the multifields search
         * @param e
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(current,
                  "This button allows you to make a \"multiple fields\" search." +
                        "\nFor use :\n\t1. Click the button\n\t2. On the opened window, choose the number of fields wanted" +
                        "\n\t3. Select your fields in the scrollable lists which is appearing (each fields need to be different)" +
                        "\n\t4. Write your query : 1 query by field, in the same order than the fields, separated by \";\"" +
                  "\nWARNING! If you write less query than the number of fields, the last query will be used for the remaining fields!" +
                        "\nIf you write more query than the number of fields, the excess queries will be ignored",
                  "Multi-fields query user manual",
                  JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.*/
    protected ImageIcon createImageIcon(String path,
                                        String description) {
        java.net.URL imgURL = getClass().getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * ListSelectionListener for the results
     */
    public static class getAbstract implements ListSelectionListener{
        Application currentApplication;

        /**
         * Constructor for this class, in order to access the params of the application
         * @param application the current application
         */
        public getAbstract(Application application){
            currentApplication = application;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int firstIndex = e.getFirstIndex();
            int lastIndex = e.getLastIndex();
            int index = 0;
            if(firstIndex != lastIndex){
                if(firstIndex == currentApplication.selectedResult){
                    index = lastIndex;
                }else{
                    index = firstIndex;
                }
            }else{
                index = firstIndex;
            }
            JTextArea wanted = new JTextArea();
            wanted.setText(currentApplication.actualResult.get(index).get("abstract"));
            wanted.setEditable(false);
            wanted.setFont(new Font("Arial", Font.BOLD, 14));
            wanted.setLineWrap(true);
            wanted.setWrapStyleWord(true);
            currentApplication.selectedResult = index;
            currentApplication.scrollableList.setViewportView(wanted);
            currentApplication.returnButton.setIcon(currentApplication.returnIcon);
            currentApplication.returnButton.setOpaque(true);
            currentApplication.returnButton.setContentAreaFilled(true);
            currentApplication.returnButton.setBorderPainted(true);
            currentApplication.returnButton.setEnabled(true);
            currentApplication.contentButton.setIcon(currentApplication.contentIcon);
            currentApplication.contentButton.setOpaque(true);
            currentApplication.contentButton.setContentAreaFilled(true);
            currentApplication.contentButton.setBorderPainted(true);
            currentApplication.contentButton.setEnabled(true);
        }
    }

    /**
     * Class to able the return button
     */
    public class returnListener implements ActionListener{
        Application currentApplication;

        /**
         * Constructor for this class, in order to access the parameters of the application
         * @param application the current application
         */
        public returnListener(Application application){
            currentApplication = application;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if(!currentApplication.seeingText) {
                currentApplication.scrollableList.setViewportView(currentApplication.list);
                returnButton.setIcon(null);
                returnButton.setOpaque(false);
                returnButton.setContentAreaFilled(false);
                returnButton.setBorderPainted(false);
                returnButton.setEnabled(false);
                currentApplication.contentButton.setIcon(null);
                currentApplication.contentButton.setOpaque(false);
                currentApplication.contentButton.setContentAreaFilled(false);
                currentApplication.contentButton.setBorderPainted(false);
                currentApplication.contentButton.setEnabled(false);
            }else{
                JTextArea wanted = new JTextArea();
                wanted.setText(currentApplication.actualResult.get(currentApplication.selectedResult).get("abstract"));
                wanted.setEditable(false);
                wanted.setFont(new Font("Arial", Font.BOLD, 14));
                wanted.setLineWrap(true);
                wanted.setWrapStyleWord(true);
                currentApplication.scrollableList.setViewportView(wanted);
                currentApplication.contentButton.setIcon(currentApplication.contentIcon);
                currentApplication.contentButton.setOpaque(true);
                currentApplication.contentButton.setContentAreaFilled(true);
                currentApplication.contentButton.setBorderPainted(true);
                currentApplication.contentButton.setEnabled(true);
                currentApplication.seeingText = false;
            }
        }
    }

    /**
     * Listener for the contentButton
     */
    public class ContentListener implements ActionListener{
        Application currentApplication;

        /**
         * Constructor for the ContentListener
         * @param application the current application
         */
        public ContentListener(Application application){
            currentApplication = application;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea wanted = new JTextArea();
            wanted.setText(currentApplication.actualResult.get(currentApplication.selectedResult).get("content"));
            wanted.setEditable(false);
            wanted.setFont(new Font("Arial", Font.BOLD, 14));
            wanted.setLineWrap(true);
            wanted.setWrapStyleWord(true);
            currentApplication.scrollableList.setViewportView(wanted);
            currentApplication.contentButton.setIcon(null);
            currentApplication.contentButton.setOpaque(false);
            currentApplication.contentButton.setContentAreaFilled(false);
            currentApplication.contentButton.setBorderPainted(false);
            currentApplication.contentButton.setEnabled(false);
            currentApplication.seeingText = true;
        }
    }

    public void launchSearch(JFrame current){
        selectedResult = 0;
        list.setListData(new Object[0]);
        scrollableList.setViewportView(list);
        returnButton.setIcon(null);
        returnButton.setOpaque(false);
        returnButton.setContentAreaFilled(false);
        returnButton.setBorderPainted(false);
        returnButton.setEnabled(false);
        contentButton.setIcon(null);
        contentButton.setOpaque(false);
        contentButton.setContentAreaFilled(false);
        contentButton.setBorderPainted(false);
        contentButton.setEnabled(false);

        List<String> l = new ArrayList<>();
        boolean synonymsChecked;
        if(synonyms.isSelected()){
            synonymsChecked = true;
        }else{
            synonymsChecked = false;
        }
        switch(numF){
            case 2:
                String selected1 = field1.getSelectedItem().toString();
                String selected2 = field2.getSelectedItem().toString();
                if(selected1 != selected2){
                    String[] fields = new String[]{selected1, selected2};
                    String queries = jtf.getText();
                    String[] split = queries.split(";");
                    String[] query = new String[2];
                    if(split.length==1){
                        query[0] = split[0];
                        query[1] = split[0];
                    }else{
                        query[0] = split[0];
                        query[1] = split[1];
                    }
                    try {
                        actualResult = main.searchMultipleFields(fields, query, synonymsChecked);
                        actualScore = main.getActualScores();
                    }catch (Exception exception){
                        exception.printStackTrace();
                    }
                }else{
                    JOptionPane.showMessageDialog(current,
                          "Please, select different fields.",
                          "Fields error",
                          JOptionPane.ERROR_MESSAGE);
                }
                break;
            case 3:
                selected1 = field1.getSelectedItem().toString();
                selected2 = field2.getSelectedItem().toString();
                String selected3 = field3.getSelectedItem().toString();
                if((!selected1.equals(selected2)) && (!selected2.equals(selected3)) && (!selected1.equals(selected3))){
                    String[] fields = new String[]{selected1, selected2, selected3};
                    String queries = jtf.getText();
                    String[] split = queries.split(";");       //NEED TO CLEAN QUERRY
                    String[] query = new String[3];
                    if(split.length==1){
                        query[0] = split[0];
                        query[1] = split[0];
                        query[2] = split[0];
                    }else if(split.length==2){
                        query[0] = split[0];
                        query[1] = split[1];
                        query[2] = split[1];
                    }else{
                        query[0] = split[0];
                        query[1] = split[1];
                        query[2] = split[2];
                    }
                    try {
                        actualResult = main.searchMultipleFields(fields, query, synonymsChecked);
                        actualScore = main.getActualScores();
                    }catch (Exception exception){
                        exception.printStackTrace();
                    }
                }else{
                    JOptionPane.showMessageDialog(current,
                          "Please, select different fields.",
                          "Fields error",
                          JOptionPane.ERROR_MESSAGE);
                }
                break;
            default:
                String selectedField = field1.getSelectedItem().toString();
                try {
                    actualResult = main.search(selectedField, jtf.getText(), synonymsChecked);
                    actualScore = main.getActualScores();
                } catch (IOException | ParseException ioException) {
                    ioException.printStackTrace();
                }
                break;
        }
        if(!actualResult.isEmpty()) {

            boolean selHist = historyTopic.isSelected();
            boolean selScien = sciencesTopic.isSelected();
            boolean selRel = religionTopic.isSelected();
            if (selHist || selScien || selRel) {

                List<Document> topRes = new ArrayList<>();
                List<Document> medRes = new ArrayList<>();
                List<Document> botRes = new ArrayList<>();
                List<Document> rest = new ArrayList<>();
                for (Document doc : actualResult) {
                    boolean histCat = isContained(docsHistory, doc);
                    boolean scienceCat = isContained(docsScience, doc);
                    boolean relCat = isContained(docsReligion, doc);

                    if (selHist && selScien && selRel) {
                        if (histCat && scienceCat && relCat) topRes.add(doc);
                        else if ((histCat && scienceCat) || (histCat && relCat) || (scienceCat && relCat))
                            medRes.add(doc);
                        else if (histCat || scienceCat || relCat) botRes.add(doc);
                        else rest.add(doc);
                    } else if (selHist && selScien) {
                        if (histCat && scienceCat) topRes.add(doc);
                        else if (histCat || scienceCat) medRes.add(doc);
                        else rest.add(doc);
                    } else if (selHist && selRel) {
                        if (histCat && relCat) topRes.add(doc);
                        else if (histCat || relCat) medRes.add(doc);
                        else rest.add(doc);
                    } else if (selScien && selRel) {
                        if (scienceCat && relCat) topRes.add(doc);
                        else if (scienceCat || relCat) medRes.add(doc);
                        else rest.add(doc);
                    } else if (selHist) {
                        if (histCat) topRes.add(doc);
                        else rest.add(doc);
                    } else if (selScien) {
                        if (scienceCat) topRes.add(doc);
                        else rest.add(doc);
                    } else {
                        if (relCat) topRes.add(doc);
                        else rest.add(doc);
                    }
                }
                topRes.addAll(medRes);
                topRes.addAll(botRes);
                topRes.addAll(rest);

                ScoreDoc[] newScore = new ScoreDoc[actualScore.length];
                int c = 0;
                for (Document doc : topRes) {
                    int index = actualResult.indexOf(doc);
                    newScore[c] = actualScore[index];
                    c++;
                }
                //refresh vars
                actualScore = newScore;
                actualResult = topRes;
            }
            int c = 0;
            for (Document doc : actualResult) {
                String tops = " [ ";
                if (isContained(docsScience, doc)) tops += "science ";
                if (isContained(docsHistory, doc)) tops += "history ";
                if (isContained(docsReligion, doc)) tops += "religion ";
                tops += "]";
                l.add(doc.get("title") + tops + " -> " + actualScore[c].score);
                c++;
            }
            list.setListData(l.toArray());
        }else{
            JTextArea noResult = new JTextArea();
            noResult.setText("There is no result for this query. Please, try again");
            noResult.setEditable(false);
            noResult.setFont(new Font("Arial", Font.ITALIC, 18));
            noResult.setForeground(Color.RED);
            noResult.setLineWrap(true);
            noResult.setWrapStyleWord(true);
            scrollableList.setViewportView(noResult);
        }

    }

    /**
     * Tell if a document belongs to a set of document
     * @param list of documents
     * @param doc unit
     * @return boolean
     */
    public boolean isContained(List<Document> list, Document doc) {

        for(Document d : list) {
            if (d.get("title").equals(doc.get("title")) && d.get("abstract").equals(doc.get("abstract"))) return true;
        }
        return false;
    }


    public static void main(String[] args) throws IOException {
        Application app = new Application();
    }
}