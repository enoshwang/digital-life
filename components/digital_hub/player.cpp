#include "player.h"
#include "qevent.h"
#include "ui_player.h"

#include <QFileDialog>
#include <QStandardPaths>
#include <QMediaPlayer>
#include <QMediaMetaData>
#include <QInputDialog>

#include "quihelper.h"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

Player::Player(QWidget *parent) :
      QWidget(parent),
      ui(new Ui::Player)
{
    SPDLOG_INFO("new Player");
    ui->setupUi(this);

    // video widget
    m_player = new QMediaPlayer(this);
    //
    ui->videoWidget->setPlayer(m_player);
    ui->videoWidget->setComBox(this->ui->comboBox);


    m_audioOutput = new QAudioOutput(this);
    m_player->setAudioOutput(m_audioOutput);
    m_player->setVideoOutput(ui->videoWidget);
    connect(m_player,&QMediaPlayer::mediaStatusChanged,this,&Player::mediaPlayerStateChanged);
    connect(m_player,&QMediaPlayer::durationChanged,this,&Player::durationChanged);
    connect(m_player,&QMediaPlayer::positionChanged,this,&Player::positionChanged);
    connect(m_player,&QMediaPlayer::playbackStateChanged,this,&Player::playbackStateChanged);
    connect(m_player,&QMediaPlayer::metaDataChanged,this,[this](){
        SPDLOG_INFO("metaDataChanged");
        if(m_player->isAvailable())
        {
            auto data = m_player->metaData();

            auto titleUrl = m_player->source();
            QFileInfo fileInfo(titleUrl.toString());
            auto title = fileInfo.baseName();
            ui->lineEdit->setText(title);
            SPDLOG_INFO("set title to {0}",title.toStdString());

            auto duration = data.value(QMediaMetaData::Duration).toInt() / 1000;
            QTime currentTime((duration / 3600) % 60, (duration / 60) % 60, duration % 60,(duration * 1000) % 1000);
            auto durationStr = currentTime.toString("hh:mm:ss");
            ui->lineEdit_6->setText(durationStr);
            SPDLOG_INFO("set duration to {0}",durationStr.toStdString());

            auto fileFormat = data.value(QMediaMetaData::FileFormat).toString();
            ui->lineEdit_2->setText(fileFormat);
            SPDLOG_INFO("set file format to {0}",fileFormat.toStdString());

            auto audioCodec = data.value(QMediaMetaData::AudioCodec).toString();
            ui->lineEdit_3->setText(audioCodec);
            SPDLOG_INFO("set audio codec to {0}",audioCodec.toStdString());

            auto videoCodec = data.value(QMediaMetaData::VideoCodec).toString();
            ui->lineEdit_4->setText(videoCodec);
            SPDLOG_INFO("set video codec to {0}",videoCodec.toStdString());

            auto videoFrameRate = data.value(QMediaMetaData::VideoFrameRate).toInt() / 1;
            ui->lineEdit_5->setText(QString::fromStdString(std::to_string(videoFrameRate)));
            SPDLOG_INFO("set video frame rate to {0}",videoFrameRate);

            auto resolution = data.value(QMediaMetaData::Resolution).toSize();
            auto resolutionStr = QString::number(resolution.width()) + tr("x") + QString::number(resolution.height());
            ui->lineEdit_7->setText(resolutionStr);
            SPDLOG_INFO("set resolution to {0}",resolutionStr.toStdString());

            auto audioBitRate = data.value(QMediaMetaData::AudioBitRate).toInt() / 1000;
            ui->lineEdit_8->setText(QString::number(audioBitRate) + tr("kbps"));
            SPDLOG_INFO("set audio bit rate to {0}",audioBitRate);

            auto videoBitRate = data.value(QMediaMetaData::VideoBitRate).toInt() / 1000;
            ui->lineEdit_9->setText(QString::number(videoBitRate) + tr("kbps"));
            SPDLOG_INFO("set video bit rate to {0}",videoBitRate);
        }
    });


    // slider
    connect(ui->horizontalSlider,&QSlider::sliderReleased,this,[this]()
    {
        auto value = this->ui->horizontalSlider->value() * 1000;
        this->m_player->setPosition(value);
        SPDLOG_INFO("set player position to {0}",value);
    });

    // open
    connect(ui->buttonOpen,&QPushButton::clicked,this,&Player::open);
    // open stream
    connect(ui->pushButtonOpenStream,&QPushButton::clicked,this,&Player::openStream);

    // play
    connect(ui->pushButtonPlay,&QPushButton::clicked,this,&Player::play);

    // stop
    connect(ui->pushButtonStop,&QPushButton::clicked,this,&Player::stop);

    // fast forward
    connect(ui->pushButtonForward,&QPushButton::clicked,this,[this]()
            {
                auto curPosition = this->m_player->position();
                this->m_player->setPosition(curPosition + 10000);
                SPDLOG_INFO("set player position to {0}",curPosition + 10000);
            });

    // rewind
    connect(ui->pushButtonRewind,&QPushButton::clicked,this,[this]()
            {
                auto curPosition = this->m_player->position();
                this->m_player->setPosition(curPosition - 10000);
                SPDLOG_INFO("set player position to {0}",curPosition - 10000);
            });

    // playback rate
    connect(ui->comboBox,QOverload<int>::of(&QComboBox::currentIndexChanged),this,[this]()
            {
                auto playbackSpeed = this->ui->comboBox->currentText().removeLast().toDouble();
                SPDLOG_INFO("Set playback rate:{0}.",playbackSpeed);
                this->m_player->setPlaybackRate(playbackSpeed);
            });

    // full screen
    connect(ui->pushButtonFullScreen, &QPushButton::clicked, this, [this]() {
        this->ui->videoWidget->setFullScreen(true);
    });

    // listview
    m_urlListModel = new QStringListModel(this);
    m_urlListModel->setStringList(m_urlList);
    ui->listView->setModel(m_urlListModel);
    // 不可编辑
    ui->listView->setEditTriggers(QAbstractItemView::NoEditTriggers);
    connect(ui->listView,&QListView::doubleClicked,this,&Player::listview_clicked);
    connect(ui->listView,&QListView::clicked,this,&Player::listview_clicked);
}

Player::~Player()
{
    SPDLOG_INFO("release Player");
    delete ui;
}

void Player::open()
{
    SPDLOG_INFO("void Player::open()");
    QFileDialog fileDialog(this);
    fileDialog.setAcceptMode(QFileDialog::AcceptOpen);
    fileDialog.setWindowTitle(tr("Open Files"));
    fileDialog.setDirectory(QStandardPaths::standardLocations(QStandardPaths::MoviesLocation).value(0,QDir::homePath()));

    if(fileDialog.exec() == QDialog::Accepted)
    {
        auto selectedUrls = fileDialog.selectedUrls();
        for(const auto& url: selectedUrls)
        {
            SPDLOG_INFO("open file:{0}",url.toString().toStdString());
            m_urlListModel->insertRow(m_urlListModel->rowCount());
            auto index = m_urlListModel->index(m_urlListModel->rowCount() - 1);
            m_urlListModel->setData(index,url.toString(),Qt::DisplayRole);
            {
                ui->listView->setCurrentIndex(index);
                m_player->setSource(url);
            }
        }
    }
    else
    {
        return;
    }
}

void Player::openStream()
{
    SPDLOG_INFO("void Player::openStream()");
    auto text = QInputDialog::getText(this, "输入框","请输入流媒体地址：",QLineEdit::Normal, "rtmp://ip:port/app name/stream code");
    if(!text.isEmpty())
    {
        SPDLOG_INFO("get input for openstream:{0}",text.toStdString());
        m_player->setSource(QUrl(text));
        m_player->play();
    }
}

void Player::play()
{
    SPDLOG_INFO("void Player::play()");

    if(ui->pushButtonPlay->text() == "暂停")
    {
        SPDLOG_INFO("push button pause clicked.");
        m_player->pause();
        ui->pushButtonPlay->setText(tr("继续"));
        return;
    }

    if(ui->pushButtonPlay->text() == "继续")
    {
        SPDLOG_INFO("push button continue clicked.");
        m_player->play();
        ui->pushButtonPlay->setText(tr("暂停"));
        return;
    }

    auto index = ui->listView->currentIndex();
    auto url = m_urlListModel->data(index).toString();
    if(url.toStdString() == "")
        return;
    SPDLOG_INFO("current url:{0}",url.toStdString());
    m_player->setSource(url);
    m_player->play();
}

void Player::stop()
{
    SPDLOG_INFO("void Player::stop()");
    m_player->stop();
}

void Player::mediaPlayerStateChanged(QMediaPlayer::MediaStatus state)
{
    SPDLOG_INFO("void Player::mediaPlayerStateChanged(QMediaPlayer::MediaStatus state)");
    switch (state) {
        case QMediaPlayer::NoMedia:
            SPDLOG_INFO("media player state change to: QMediaPlayer::NoMedia");
            break;
        case QMediaPlayer::LoadingMedia:
            SPDLOG_INFO("media player state change to: QMediaPlayer::LoadingMedia");
            break;
        case QMediaPlayer::LoadedMedia:
            SPDLOG_INFO("media player state change to: QMediaPlayer::LoadedMedia");
            break;
        case QMediaPlayer::StalledMedia:
            SPDLOG_INFO("media player state change to: QMediaPlayer::StalledMedia");
            break;
        case QMediaPlayer::BufferingMedia:
            SPDLOG_INFO("media player state change to: QMediaPlayer::BufferingMedia");
            break;
        case QMediaPlayer::BufferedMedia:
            SPDLOG_INFO("media player state change to: QMediaPlayer::BufferedMedia");
            break;
        case QMediaPlayer::EndOfMedia:
            SPDLOG_INFO("media player state change to: QMediaPlayer::EndOfMedia");
            break;
        case QMediaPlayer::InvalidMedia:
            SPDLOG_WARN("media player state change to: QMediaPlayer::InvalidMedia");
            QUIHelper::showMessageBox("无效媒体",MsgBoxType::WARN_TYPE);
            break;
        default:
            break;
    }
}

void Player::durationChanged(qint64 duration)
{
    SPDLOG_INFO("void Player::durationChanged(qint64 duration)");
    m_duration = duration / 1000;
    ui->horizontalSlider->setMaximum(m_duration);
    SPDLOG_INFO("set slider maximum to:{0}",m_duration);
}

void Player::positionChanged(qint64 progress)
{
    if(!ui->horizontalSlider->isSliderDown())
        ui->horizontalSlider->setValue(progress / 1000);
    updateDurationInfo(progress / 1000);
}

void Player::playbackStateChanged(QMediaPlayer::PlaybackState state)
{
    SPDLOG_INFO("void Player::playbackStateChanged(QMediaPlayer::PlaybackState state)");
    switch (state) {
        case QMediaPlayer::StoppedState:
            SPDLOG_INFO("player playback state change to: QMediaPlayer::StoppedState");
            ui->pushButtonPlay->setText(tr("播放"));
            break;
        case QMediaPlayer::PlayingState:
            SPDLOG_INFO("player playback state change to: QMediaPlayer::PlayingState");
            ui->pushButtonPlay->setText(tr("暂停"));
            break;
        case QMediaPlayer::PausedState:
            SPDLOG_INFO("player playback state change to: QMediaPlayer::PausedState");
            ui->pushButtonPlay->setText(tr("播放"));
            break;
        default:
            break;
    }
}

void Player::listview_clicked(const QModelIndex &index)
{
    SPDLOG_INFO("listview index:{0} clicked/double clicked",m_urlListModel->data(index).toString().toStdString());
    auto url = m_urlListModel->data(index).toString();
    m_player->setSource(url);
}

void Player::updateDurationInfo(qint64 currentInfo)
{
    QString tStr;
    if (currentInfo || m_duration)
    {
        QTime currentTime((currentInfo / 3600) % 60, (currentInfo / 60) % 60, currentInfo % 60,(currentInfo * 1000) % 1000);
        QTime totalTime((m_duration / 3600) % 60, (m_duration / 60) % 60, m_duration % 60,(m_duration * 1000) % 1000);
        QString format = "mm:ss";
        if (m_duration > 3600)
            format = "hh:mm:ss";
        tStr = currentTime.toString(format) + " / " + totalTime.toString(format);
    }
    ui->labelDuration->setText(tStr);
}

bool Player::eventFilter(QObject *watched, QEvent *event)
{
    SPDLOG_INFO("Start Player::eventFilter");
    if (watched == ui->videoWidget && event->type() == QEvent::MouseButtonDblClick)
    {
        SPDLOG_INFO("mouse button dbl click on video widget");
        QMouseEvent *mouseEvent = static_cast<QMouseEvent *>(event);
        if (mouseEvent->button() == Qt::LeftButton)
        {
            SPDLOG_INFO("mouse button click on left button");
            if(ui->videoWidget->isFullScreen())
            {
                SPDLOG_INFO("mouse button click on left button,and now is full screen,will exit full screen");
                ui->videoWidget->setFullScreen(false);
            }
            return true;
        }
    }

    return QWidget::eventFilter(watched, event);
}


