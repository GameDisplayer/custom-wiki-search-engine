import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Application extends JFrame {
    private JPanel container = new JPanel();
    private JTextField jtf = new JTextField("Search the Wiki world");
    private JButton jb = new JButton("GO");
    private Object[] fields = new Object[]{"title", "abstract", "content"};
    private JComboBox field = new JComboBox(fields);
    JList list = new JList();
    JScrollPane scrollableList = new JScrollPane(list);

    public Application() {
        this.setTitle("Wiki Search");
        this.setSize(750, 620);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        container.setBackground(Color.white);
        container.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        JPanel med = new JPanel();
        JPanel filters = new JPanel();
        JPanel choosable = new JPanel();

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

        jb.addActionListener(new ButtonListener());
        top.add(jb);

        list.setFont(police);
        scrollableList.setPreferredSize(new Dimension(700, 500));
        scrollableList.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        med.add(scrollableList);

        JLabel filter = new JLabel("Filters :", JLabel.CENTER);
        filter.setFont(police);
        filters.add(filter);

        JLabel content = new JLabel("Field :", JLabel.LEFT);
        content.setFont(new Font("Arial", Font.ITALIC, 14));
        field.setPreferredSize(new Dimension(70, 20));
        field.setSelectedIndex(2);
        choosable.add(content);
        choosable.add(field);

        container.add(top, BorderLayout.NORTH);
        container.add(med, BorderLayout.SOUTH);
        container.add(filters, BorderLayout.WEST);
        container.add(choosable, BorderLayout.CENTER);


        this.setContentPane(container);
        this.setVisible(true);
    }


    class ButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Main main = new Main();
            String selectedField = field.getSelectedItem().toString();
            try {
                List<String> l = main.search(selectedField, jtf.getText());
                list.setListData(l.toArray());
            } catch (IOException | ParseException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    class JtextFileEnterListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_ENTER){
                Main main = new Main();
                String selectedField = field.getSelectedItem().toString();
                try {
                    List<String> l = main.search(selectedField, jtf.getText());
                    list.setListData(l.toArray());
                } catch (IOException | ParseException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }


    public static void main(String[] args) {
        Application app = new Application();
    }
}
