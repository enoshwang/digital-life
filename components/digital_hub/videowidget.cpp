#include "videowidget.h"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>


VideoWidget::VideoWidget(QWidget *parent): QVideoWidget(parent)
{
    this->speedLabel = new QLabel(this);
    this->speedLabel->setStyleSheet("QLabel { background-color : rgba(0, 0, 0, 128); color : white; padding: 5px; }");
    speedLabel->setAlignment(Qt::AlignCenter);
    speedLabel->hide();

    this->timer = new QTimer(this);
    this->timer->setSingleShot(true);
    connect(timer,&QTimer::timeout, speedLabel, &QLabel::hide);
}

void VideoWidget::setPlayer(QMediaPlayer *p)
{
    this->player = p;
}

void VideoWidget::setComBox(QComboBox *cb)
{
    this->comboBox = cb;
}

void VideoWidget::displaySpeed(QString playbackSpeed)
{
    auto text = QString("Playback Speed: ") + playbackSpeed;
    speedLabel->setText(text);
    speedLabel->adjustSize();
    speedLabel->setFixedSize(speedLabel->sizeHint());
    speedLabel->move((width() - speedLabel->width()) / 2, (height() - speedLabel->height()) / 2);
    speedLabel->show();
    timer->start(2000);
    SPDLOG_INFO("Speed label show:{0}",text.toStdString());
}

void VideoWidget::keyPressEvent(QKeyEvent *event)
{
    if ((event->key() == Qt::Key_Escape || event->key() == Qt::Key_Back) && isFullScreen())
    {
        setFullScreen(false);
        event->accept();
    }
    else if (event->key() == Qt::Key_Enter && event->modifiers() & Qt::Key_Alt)
    {
        setFullScreen(!isFullScreen());
        event->accept();
    }
    else if (event->key() == Qt::Key_Left)
    {
        this->player->setPosition(this->player->position() - 10000);
    }
    else if (event->key() == Qt::Key_Right)
    {
        // 前进 10 秒
        this->player->setPosition(this->player->position() + 10000);
    }
    else if (event->key() == Qt::Key_Up)
    {
        int currentIndex = this->comboBox->currentIndex();
        int nextIndex = (currentIndex + 1 < comboBox->count()) ? currentIndex + 1 : currentIndex;
        comboBox->setCurrentIndex(nextIndex);
        SPDLOG_INFO("Set index:{0}",nextIndex);
        displaySpeed(this->comboBox->currentText());
    }
    else if (event->key() == Qt::Key_Down)
    {
        int currentIndex = comboBox->currentIndex();
        int prevIndex = (currentIndex - 1 >= 0) ? currentIndex - 1 : currentIndex;
        comboBox->setCurrentIndex(prevIndex);
        SPDLOG_INFO("Set index:{0}",prevIndex);
        displaySpeed(this->comboBox->currentText());
    }
    else
    {
        QVideoWidget::keyPressEvent(event);
    }
}

void VideoWidget::mouseDoubleClickEvent(QMouseEvent *event)
{
    SPDLOG_INFO("video widet: mouse double click");
    setFullScreen(!isFullScreen());
    event->accept();
}

void VideoWidget::mousePressEvent(QMouseEvent *event)
{
    QVideoWidget::mousePressEvent(event);
}
