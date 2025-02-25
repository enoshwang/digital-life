#include "browserdataanalysis.h"
#include "ui_browserdataanalysis.h"

#include <QString>
#include <QStringList>
#include <QtSql/QSqlDatabase>
#include <QtSql/QSqlError>
#include <QtSql/QSqlQuery>

#include <algorithm>

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

using namespace std::string_literals;

BrowserDataAnalysis::BrowserDataAnalysis(QWidget *parent) :
      QMainWindow(parent),
      ui(new Ui::BrowserDataAnalysis),m_str_list_model(new QStringListModel(this))
{
    ui->setupUi(this);

    (void)getBrowserData();

    m_analysis_type_map.insert(tr("浏览次数前十"),TopTen);
    m_analysis_type_map.insert(tr("每日访问次数"),VisitsPerDay);

    QStringList str_list = m_analysis_type_map.keys();
    std::sort(str_list.begin(),str_list.end());

    m_str_list_model->setStringList(str_list);

    ui->listView->setModel(m_str_list_model);
    ui->listView->setCurrentIndex(m_str_list_model->index(0));


    m_chart_widget_layout = new QVBoxLayout(ui->widget);
    m_chart_view = new QChartView(ui->widget);
    // 启用抗锯齿效果。在绘制图表时，边缘会更加平滑，图表看起来更加美观。这在绘制包含曲线或圆弧等曲线图形时特别有用，因它可以减少锯齿状边缘的出现。
    m_chart_view->setRenderHint(QPainter::Antialiasing);
    m_chart_widget_layout->addWidget(m_chart_view);
    ui->widget->setLayout(m_chart_widget_layout);

    connect(ui->listView->selectionModel(),&QItemSelectionModel::currentChanged,this,
            [this](const QModelIndex& index){
                setActiveAnalysisType(m_analysis_type_map[m_str_list_model->data(index).toString()]);
            });
    SPDLOG_INFO("connect listview to changed");

    setActiveAnalysisType(m_analysis_type_map[str_list[0]]);
}

BrowserDataAnalysis::~BrowserDataAnalysis()
{
    delete ui;
}

void BrowserDataAnalysis::setActiveAnalysisType(AnalysisType type)
{
    switch(type)
    {
        case TopTen:
            if(!m_top_ten)
                m_top_ten = createTopTen();
            m_chart_view->setChart(m_top_ten);
            break;
        case VisitsPerDay:
            if(!m_visits_per_day_chart)
                m_visits_per_day_chart = createVisitsPerDayChart();
            m_chart_view->setChart(m_visits_per_day_chart);
            break;
        default:
            if(!m_top_ten)
                m_top_ten = createTopTen();
            m_chart_view->setChart(m_top_ten);
            break;
    }
}

void BrowserDataAnalysis::getBrowserData()
{
    SPDLOG_INFO("start get browser data.");
    QSqlDatabase db = QSqlDatabase::addDatabase("QSQLITE");
    db.setDatabaseName("History");
    if(db.open())
    {
        // top ten
        {
            QSqlQuery query;
            query.exec("SELECT url, visit_count FROM urls ORDER BY visit_count DESC LIMIT 10;");

            while (query.next())
            {
                std::string url = query.value(0).toString().toStdString();
                int visitCount = query.value(1).toInt();
                m_topTen[url] = visitCount;
                SPDLOG_INFO("url:{0} ,count:{1}",url,visitCount);
            }
        }
        db.close();
    }
    else
    {
        SPDLOG_WARN("db open error:{0}",db.lastError().text().toStdString());
    }

}

QChart *BrowserDataAnalysis::createTopTen()
{
    QChart *chart = new QChart();
    chart->setTitle("浏览次数前十");

    QPieSeries *series = new QPieSeries(chart);
    series->setName("url");

    if(m_topTen.size() == 0)
        return chart;

    for(const auto& i : m_topTen)
    {
        series->append(i.first.c_str(),i.second);
    }
    series->setLabelsVisible();
    series->setPieSize(0.6);

    for(int i=0;i<m_topTen.size();++i)
    {
        auto slice_index = series->slices().at(i);
        slice_index->setLabelVisible();
        slice_index->setExploded();
        slice_index->setExplodeDistanceFactor(0.1);
    }

    chart->setAnimationOptions(QChart::AllAnimations);
    chart->legend()->setAlignment(Qt::AlignRight);

    chart->addSeries(series);
    chart->setTheme(QChart::ChartThemeBlueIcy);
    return chart;
}

QChart *BrowserDataAnalysis::createVisitsPerDayChart()
{
    QChart *chart = new QChart();
    chart->setTitle("每日访问次数");

    // first
    QLineSeries *series = new QLineSeries();
    series->setName(tr("first"));
    series->append(0, 6);
    series->append(1, 4);
    series->append(2, 8);
    series->append(3, 4);
    series->append(4, 5);

    // second
    QLineSeries *series2 = new QLineSeries();
    series2->setName(tr("second"));
    series2->append(0, 5);
    series2->append(1, 23);
    series2->append(2, 1);
    series2->append(3, 54);
    series2->append(4, 2);

    // third
    QLineSeries *series3 = new QLineSeries();
    series3->setName(tr("third"));
    series3->append(0, 5);
    series3->append(1, 23);
    series3->append(2, 1);
    series3->append(3, 54);
    series3->append(4, 2);

    // total
    QLineSeries *series4 = new QLineSeries();
    series4->setName(tr("total"));
    series4->append(0, 20);
    series4->append(1, 30);
    series4->append(2, 40);
    series4->append(3, 30);
    series4->append(4, 20);

    chart->legend()->setAlignment(Qt::AlignRight);

    chart->addSeries(series);
    chart->addSeries(series2);
    chart->addSeries(series3);
    chart->addSeries(series4);

    chart->createDefaultAxes();

    return chart;
}
