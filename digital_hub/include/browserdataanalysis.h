#ifndef BROWSERDATAANALYSIS_H
#define BROWSERDATAANALYSIS_H

#include <QMainWindow>
#include <QStringListModel>
#include <QtCharts/QtCharts>
#include <QHash>
#include <QDate>

#include <string>
#include <unordered_map>


namespace Ui
{
    class BrowserDataAnalysis;
}

class BrowserDataAnalysis : public QMainWindow
{
    Q_OBJECT

public:
    explicit BrowserDataAnalysis(QWidget *parent = nullptr);
    ~BrowserDataAnalysis();

private:
    Ui::BrowserDataAnalysis *ui;

private:
    enum AnalysisType {
        TopTen,
        VisitsPerDay
    };

    void setActiveAnalysisType(AnalysisType type);

    QHash<QString,AnalysisType> m_analysis_type_map;
    QStringListModel* m_str_list_model {nullptr};

private:
    void getBrowserData();

    QChart* createTopTen();
    QChart* createVisitsPerDayChart();

    QChart *m_top_ten {nullptr};
    std::unordered_map<std::string,int> m_topTen;

    QChart *m_visits_per_day_chart {nullptr};

    QChartView* m_chart_view {nullptr};
    QVBoxLayout* m_chart_widget_layout {nullptr};
};

#endif // BROWSERDATAANALYSIS_H
