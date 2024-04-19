package org.tera201.vcstoolkit.info;

import com.formdev.flatlaf.FlatClientProperties;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import net.miginfocom.swing.MigLayout;
import org.repodriller.scm.BlameManager;
import org.repodriller.scm.CommitSize;
import org.tera201.vcstoolkit.panels.CommitPanel;
import org.tera201.vcstoolkit.tabs.GitTab;
import org.tera201.vcstoolkit.tabs.TabEnum;
import org.tera201.vcstoolkit.tabs.TabManager;
import org.tera201.vcstoolkit.utils.DateCalculator;
import org.tera201.swing.chart.ChartLegendRenderer;
import org.tera201.swing.chart.data.category.DefaultCategoryDataset;
import org.tera201.swing.chart.data.pie.DefaultPieDataset;
import org.tera201.swing.chart.line.LineChart;
import org.tera201.swing.chart.pie.PieChart;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class InfoPage {
    private JPanel panel1;
    private JPanel chartPanel;
    private JPanel commitPanel;
    private JScrollPane commitScrollPanel;
    private JScrollPane linechartScrollPane;
    private JLabel authorLabel;
    private JLabel curAuthorLabel;
    private JLabel rowsLabel;
    private JLabel sizeLabel;
    private JLabel revisionLabel;
    LineChart lineChart = new LineChart();
    private TabManager tabManager;

    public InfoPage(TabManager tabManager) {
        this.tabManager = tabManager;
        GitTab gitTab = (GitTab) tabManager.getTabMap().get(TabEnum.GIT);
        Map<String, CommitSize> commitSizeMap = gitTab.getMyRepo().getScm().repositorySize();
        BlameManager blameManager  = gitTab.getMyRepo().getScm().blameManager();
//        authorLabel.setText(blameManager.getRootPackageInfo().);
        rowsLabel.setText(String.valueOf(blameManager.getRootPackageInfo().getLineCount()));
        sizeLabel.setText(String.valueOf(blameManager.getRootPackageInfo().getLineSize()));
        revisionLabel.setText(blameManager.getRootPackageInfo().findLatestCommit().name());

        chartPanel.putClientProperty(FlatClientProperties.STYLE, ""
                        + "border:5,5,5,5;"
                        + "background:null");
        chartPanel.setLayout(new MigLayout("wrap,fill,gap 10", "fill"));

        PieChart pieChart1 = new PieChart();
        JLabel header1 = new JLabel("Authors impact");
        header1.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+1");
        pieChart1.setHeader(header1);
        pieChart1.getChartColor().addColor(Color.decode("#f87171"), Color.decode("#fb923c"), Color.decode("#fbbf24"), Color.decode("#a3e635"), Color.decode("#34d399"), Color.decode("#22d3ee"), Color.decode("#818cf8"), Color.decode("#c084fc"));
        pieChart1.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5,$Component.borderColor,,20");
        pieChart1.setDataset(createPieData(blameManager));
        chartPanel.add(pieChart1, "split 3,height 290");

        JSplitPane splitPane = new JSplitPane();
        JBSplitter splitter = new JBSplitter(false, 0.95f);
        splitter.setDividerWidth(1);

        splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        Map<Integer, CommitPanel> commitPanels = new HashMap<>();

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> yearList = new JBList<>(listModel);
        yearList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        Calendar calendar = Calendar.getInstance();
        commitSizeMap.values().stream().map(commitSize -> {
            Date date = new Date((long) commitSize.getDate() * 1000);
            calendar.setTime(date);
            return calendar.get(Calendar.YEAR);
        }).collect(Collectors.toSet()).stream().sorted(Comparator.reverseOrder()).forEach(year -> {
            listModel.addElement(Integer.toString(year));
            commitPanels.put(year, new CommitPanel(year));
        });

        commitSizeMap.values().forEach(commitSize -> {
            Date date = new Date((long) commitSize.getDate() * 1000);
            calendar.setTime(date);
            int year = calendar.get(Calendar.YEAR);
            int day = calendar.get(Calendar.DAY_OF_YEAR);
            commitPanels.get(year).addCommitCountForDay(day, 1);
        });
        yearList.setSelectedIndex(0);

        JScrollPane listScrollPane = new JBScrollPane(yearList);

        splitter.setFirstComponent(commitPanels.get(Integer.parseInt(yearList.getSelectedValue())));
        splitter.setSecondComponent(listScrollPane);

        yearList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                    splitter.setFirstComponent(commitPanels.get(Integer.parseInt(yearList.getSelectedValue())));
            }
        });


        commitScrollPanel.setViewportView(splitter);


        lineChart = new LineChart();
        lineChart.setChartType(LineChart.ChartType.CURVE);
        lineChart.putClientProperty(FlatClientProperties.STYLE, ""
                + "border:5,5,5,5,$Component.borderColor,,20");
        linechartScrollPane.setViewportView(lineChart);
        createLineChartData(commitSizeMap.values());
    }

    private DefaultPieDataset createPieData(BlameManager blameManager) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        blameManager.getRootPackageInfo().getAuthorInfo().forEach((s, blameAuthorInfo) -> {dataset.addValue(s, blameAuthorInfo.getLineCount());});
        return dataset;
    }

    private void createLineChartData(Collection<CommitSize> commitSizeCollection) {
        DefaultCategoryDataset<String, String> categoryDataset = new DefaultCategoryDataset<>();
        SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy");
        commitSizeCollection.stream().collect(Collectors.groupingBy(
                e -> df.format(new Date((long) e.getDate() * 1000)),
                Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparingInt(CommitSize::getDate)),
                        optionalEvent -> optionalEvent.orElse(null)
                )
        )).values().stream().sorted(Comparator.comparingInt(CommitSize::getDate)).forEach(commitSize -> {
            Date date = new Date((long) commitSize.getDate() * 1000);
            String formattedDate = df.format(date);
            categoryDataset.addValue(commitSize.getProjectSize(), "Project ", formattedDate);
        });

        /**
         * Control the legend we do not show all legend
         */
        try {
            Date date = df.parse(categoryDataset.getColumnKey(0));
            Date dateEnd = df.parse(categoryDataset.getColumnKey(categoryDataset.getColumnCount() - 1));

            DateCalculator dcal = new DateCalculator(date, dateEnd);
            long diff = dcal.getDifferenceDays();
            long valuesCount = categoryDataset.getColumnKeys().size();

            double d = Math.ceil((valuesCount / 20f));
            lineChart.setLegendRenderer(new ChartLegendRenderer() {
                @Override
                public Component getLegendComponent(Object legend, int index) {
                    if (index % d == 0) {
                        return super.getLegendComponent(legend, index);
                    } else {
                        return null;
                    }
                }
            });
        } catch (ParseException e) {
            System.err.println(e);
        }

        lineChart.setValuesFormat(new DecimalFormat("#,##0.## B"));

        lineChart.setCategoryDataset(categoryDataset);
        lineChart.getChartColor().addColor(Color.decode("#38bdf8"), Color.decode("#fb7185"), Color.decode("#34d399"));
        JLabel header = new JLabel("Project Size Evolution");
        header.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:+1;"
                + "border:0,0,5,0");
        lineChart.setHeader(header);
    }

    public JComponent getComponent() {
        return panel1;
    }
}
