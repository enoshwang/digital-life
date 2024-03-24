#ifndef PLAYER_H
#define PLAYER_H

#include <QWidget>

#include <QStringList>
#include <QStringListModel>
#include <QMediaPlayer>
#include <QAudioOutput>

namespace Ui {
class Player;
}

class Player : public QWidget
{
    Q_OBJECT

public:
    explicit Player(QWidget *parent = nullptr);
    ~Player();

private slots:
    void open();
    void openStream();
    void play();
    void stop();
    void mediaPlayerStateChanged(QMediaPlayer::MediaStatus state);
    void durationChanged(qint64 duration);
    void positionChanged(qint64 progress);
    void playbackStateChanged(QMediaPlayer::PlaybackState state);

    void listview_clicked(const QModelIndex& index);

private:
    void updateDurationInfo(qint64 currentInfo);

protected:
    bool eventFilter(QObject *watched, QEvent *event) override;

private:
    Ui::Player *ui;

    QMediaPlayer* m_player {nullptr};
    QAudioOutput *m_audioOutput {nullptr};

    QStringList m_urlList{};
    QStringListModel* m_urlListModel;

    // slider
    qint64 m_duration;
};

#endif // PLAYER_H
