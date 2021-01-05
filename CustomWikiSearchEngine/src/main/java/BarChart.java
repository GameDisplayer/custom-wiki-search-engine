import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BarChart extends ApplicationFrame {

    public BarChart(String applicationTitle, String chartTitle) throws IOException {
        super(applicationTitle);
        JFreeChart barChart = ChartFactory.createBarChart (
                chartTitle,
                "Topic",
                "Occurences",
                createDataset(),
                PlotOrientation.VERTICAL,
                true, true, false);

        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setPreferredSize(new Dimension(560, 367));
        setContentPane(chartPanel);

    }

    private void generateDatasetFromHashMap(DefaultCategoryDataset dataset, HashMap<String, Integer> hm, String topic) {
        int count = 0;
        for (Map.Entry<String, Integer> en : hm.entrySet()) {
            int decount = hm.size() - count;
            if(decount <= 10 ) dataset.addValue(en.getValue(), en.getKey(), topic);
            count++;
        }
    }

    private CategoryDataset createDataset() throws IOException {
        final String history = "History";
        final String science = "Sciences";
        final String religion = "Religion & Belief";

        TopicModeling tp = new TopicModeling("abtsract");

        final DefaultCategoryDataset dataset =
                new DefaultCategoryDataset( );

        generateDatasetFromHashMap(dataset, tp.hmHist, history);
        generateDatasetFromHashMap(dataset, tp.hmScienc, science);
        generateDatasetFromHashMap(dataset, tp.hmRel, religion);
        
        return dataset;
    }

    public static void main( String[ ] args ) throws IOException {
        BarChart chart = new BarChart("WikiDump Topics Statistics",
                "Top 10 words per topic");
        chart.pack( );
        //RefineryUtilities.centerFrameOnScreen( chart );
        chart.setVisible( true );
    }
}

