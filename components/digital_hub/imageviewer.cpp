#include "imageviewer.h"
#include "ui_imageviewer.h"
#include "quihelper.h"

#define SPDLOG_ACTIVE_LEVEL SPDLOG_LEVEL_TRACE
#include <spdlog/spdlog.h>

#include <QFileDialog>
#include <QStandardPaths>
#include <QImageReader>
#include <QColorSpace>
#include <QLabel>

ImageViewer::ImageViewer(QWidget *parent):QMainWindow(parent),ui(new Ui::ImageViewer)
{
    ui->setupUi(this);

    connect(ui->actionopen,&QAction::triggered,this,&ImageViewer::slotsActionOpen);
    SPDLOG_DEBUG("image viewer construct ok");
}

ImageViewer::~ImageViewer()
{
    delete ui;
}

void ImageViewer::slotsActionOpen()
{
    QFileDialog fileDialog(this);
    fileDialog.setAcceptMode(QFileDialog::AcceptOpen);
    fileDialog.setWindowTitle(tr("打开文件"));
    fileDialog.setDirectory(QStandardPaths::standardLocations(QStandardPaths::PicturesLocation).value(0,QDir::homePath()));

    if(fileDialog.exec() == QDialog::Accepted)
    {
        loadFile(fileDialog.selectedFiles().constFirst());
    }
    else
    {
        return;
    }
}

void ImageViewer::loadFile(const QString &fileName)
{
    QImageReader reader(fileName);
    reader.setAutoTransform(true);
    const QImage newImage = reader.read();

    if (newImage.isNull())
    {
        QUIHelper::showMessageBox(tr("Cannot load %1: %2").arg(QDir::toNativeSeparators(fileName), reader.errorString()),MsgBoxType::WARN_TYPE);
        return;
    }

    setImage(newImage);
    setWindowFilePath(fileName);

    const QString description = image.colorSpace().isValid() ? image.colorSpace().description() : tr("unknown");
    const QString message = tr("Opened \"%1\", %2x%3, Depth: %4 (%5)").arg(QDir::toNativeSeparators(fileName)).arg(image.width()).arg(image.height()).arg(image.depth()).arg(description);
    statusBar()->showMessage(message);
}

void ImageViewer::setImage(const QImage &newImage)
{
    image = newImage;
    if (image.colorSpace().isValid())
        image.convertToColorSpace(QColorSpace::SRgb);

    ui->imagelabel->setPixmap(QPixmap::fromImage(image));
    m_scale_factor = 1.0;
    ui->imagelabel->adjustSize();
}
