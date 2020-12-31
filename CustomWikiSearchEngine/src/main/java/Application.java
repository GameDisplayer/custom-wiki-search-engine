import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.omg.CORBA.BAD_INV_ORDER;
import org.w3c.dom.ls.LSOutput;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * Class used to create an user interface
 */
public class Application extends JFrame {
    private JPanel container = new JPanel();
    private JTextField jtf = new JTextField("Search the Wiki world");
    private JButton jb = new JButton("GO");
    private JCheckBox numFields = new JCheckBox("Multiple field", false);
    private Object[] fields = new Object[]{"title", "abstract", "content"};
    private JComboBox field1 = new JComboBox(fields);
    private JComboBox field2 = new JComboBox(fields);
    private JLabel content2 = new JLabel("and", JLabel.LEFT);
    JList list = new JList();
    JScrollPane scrollableList = new JScrollPane(list);

    /**
     * Constructor for the application
     */
    public Application() {
        this.setTitle("Wiki Search");
        this.setSize(750, 650);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        container.setBackground(Color.white);
        container.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        JPanel med = new JPanel();
        //Panel just for the string "Filters:"
        JPanel text = new JPanel();
        //Panel for the checkBox
        JPanel multFields = new JPanel();
        //Panel for choosing the field(s)
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.LINE_AXIS));
        //Panel for all the fields settings (checkBox + choose)
        JPanel choosable = new JPanel();
        choosable.setLayout(new BoxLayout(choosable, BoxLayout.Y_AXIS));
        //Panel with all the filters
        JPanel filters = new JPanel(new BorderLayout());

        //Search bar :
        Font police = new Font("Arial", Font.BOLD, 14);
        jtf.setFont(police);
        jtf.setPreferredSize(new Dimension(650, 30));
        jtf.setForeground(Color.GRAY);
        top.add(jtf);
        jtf.addKeyListener(new JtextFileEnterListener());
        jtf.addMouseListener(new MouseAdapter(){
            int numClick = 0;
            @Override
            public void mouseClicked(MouseEvent e){
                numClick += 1;
                if(numClick == 1) {
                    jtf.setText("");
                }
            }
        });

        //Go button :
        jb.addActionListener(new ButtonListener());
        top.add(jb);

        //Result part :
        list.setFont(police);
        scrollableList.setPreferredSize(new Dimension(700, 500));
        scrollableList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        med.add(scrollableList);

        //String "Filters:" :
        JLabel filterString = new JLabel("Filters :", JLabel.CENTER);
        filterString.setFont(police);
        text.add(filterString);

        //Selection of the fields :
        JLabel content = new JLabel("Field :", JLabel.LEFT);
        content.setFont(new Font("Arial", Font.ITALIC, 14));
        content.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        field1.setPreferredSize(new Dimension(80, 20));
        field1.setSelectedIndex(2);
        field1.setMaximumSize(new Dimension(80, 20));
        field1.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        content2.setFont(new Font("Arial", Font.ITALIC, 14));
        content2.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        content2.setVisible(false);
        field2.setPreferredSize(new Dimension(80, 20));
        field2.setSelectedIndex(2);
        field2.setMaximumSize(new Dimension(80, 20));
        field2.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        field2.setVisible(false);
        fields.add(content);
        fields.add(Box.createHorizontalStrut(5));
        fields.add(field1);
        fields.add(Box.createHorizontalStrut(5));
        fields.add(content2);
        fields.add(Box.createHorizontalStrut(5));
        fields.add(field2);

        //CheckBox for multipel fields :
        numFields.setAlignmentX(Component.CENTER_ALIGNMENT);
        numFields.addItemListener(new MultiFieldsListener());
        numFields.addActionListener(new PopUpInformation(this));
        //helpButton.setPreferredSize(new Dimension(32,32));
        ImageIcon helpIcon = createImageIcon("resources/Icon/help.png", "help");
        JButton helpButton = new JButton(helpIcon);
        multFields.add(helpButton);
        multFields.add(numFields);

        //Filter regarding the fields :
        multFields.setAlignmentX(Component.CENTER_ALIGNMENT);
        multFields.setAlignmentY(Component.TOP_ALIGNMENT);
        choosable.add(multFields);
        fields.setAlignmentX(Component.CENTER_ALIGNMENT);
        choosable.add(fields);

        //All the filters :
        filters.add(choosable, BorderLayout.WEST);

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
    class ButtonListener implements ActionListener {
        /**
         * Function allowing the search when clicking on the button
         * @param e the click
         */
        public void actionPerformed(ActionEvent e) {
            Main main = new Main();
            String selectedField = field1.getSelectedItem().toString();
            try {
                List<String> l = main.search(selectedField, jtf.getText());
                list.setListData(l.toArray());
            } catch (IOException | ParseException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Listener for the search bar
     */
    class JtextFileEnterListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {}

        /**
         * Function allowing the search when "Enter" is pressed
         * @param e the key pressed
         */
        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_ENTER){
                Main main = new Main();
                String selectedField = field1.getSelectedItem().toString();
                try {
                    List<String> l = main.search(selectedField, jtf.getText());
                    list.setListData(l.toArray());
                } catch (IOException | ParseException ioException) {
                    ioException.printStackTrace();
                }
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
            if(e.getStateChange() == ItemEvent.SELECTED){
                content2.setVisible(true);
                field2.setVisible(true);
            }else{
                content2.setVisible(false);
                field2.setVisible(false);
            }
        }
    }

    /**
     * ActionListener for the checkButton
     */
    public class PopUpInformation implements ActionListener{
        private JFrame current;

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
            if(numFields.isSelected()){
                JOptionPane.showMessageDialog(current,
                      "Eggs are not supposed to be green.",
                      "Information",
                      JOptionPane.PLAIN_MESSAGE);
            }
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

    public static void main(String[] args) {
        Application app = new Application();
    }
}
