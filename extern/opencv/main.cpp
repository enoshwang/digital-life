#include <opencv2/core.hpp>
#include <opencv2/highgui.hpp>

#include <iostream>
#include <random>

#include "ImageSimilarity.hpp"

// 添加椒盐噪声
void saltAndPepperNoise(cv::Mat image, int saltAndPepperPercentage);
// 减色
void colorReduce(cv::Mat image, int div);
// 锐化
void sharpen2D(cv::Mat image,cv::Mat result);

int main(int argc, char** argv)
{
    // 创建一个空图像
    //cv::Mat image(400,600,CV_8UC3,cv::Scalar(0,0,255));

    cv::Mat image = cv::imread("C:/Users/enosh/Pictures/test.jpg", cv::IMREAD_COLOR);

    cv::Mat result = image.clone();
    sharpen2D(image, result);

    // 显示图像
    cv::imshow("Display window", result);
    cv::waitKey(0);

    return 0;
}

void saltAndPepperNoise(cv::Mat image, int saltAndPepperPercentage)
{
    if (saltAndPepperPercentage < 0 || saltAndPepperPercentage > 100)
    {
        return;
    }

    auto saltAndPepperCount = static_cast<int>(image.rows * image.cols * saltAndPepperPercentage / 100.0f);

    auto height = image.rows;
    auto width = image.cols;

    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> rowDist(0, height - 1);
    std::uniform_int_distribution<> colDist(0, width - 1);

    for (int i = 0; i < saltAndPepperCount; i++)
    {
        auto row = rowDist(gen);
        auto col = colDist(gen);
        image.at<cv::Vec3b>(row, col)[0] = 0;
        image.at<cv::Vec3b>(row, col)[1] = 0;
        image.at<cv::Vec3b>(row, col)[2] = 0;
    }
}

void colorReduce(cv::Mat image, int div)
{
    if (div <= 1 || div > 255)
    {
        return;
    }

    /*
    auto rows = image.rows;
    auto elementsOfRow = image.cols;
    for (int i = 0; i < rows; i++)
    {
        for (int j = 0; j < elementsOfRow; j++)
        {
            image.at<cv::Vec3b>(i, j)[0] = image.at<cv::Vec3b>(i, j)[0] / div * div + div / 2;
            image.at<cv::Vec3b>(i, j)[1] = image.at<cv::Vec3b>(i, j)[1] / div * div + div / 2;
            image.at<cv::Vec3b>(i, j)[2] = image.at<cv::Vec3b>(i, j)[2] / div * div + div / 2;
        }
    }
    */

   // 使用迭代器
    auto it = image.begin<cv::Vec3b>();
    auto end = image.end<cv::Vec3b>();
    for (; it != end; ++it)
    {
        (*it)[0] = (*it)[0] / div * div + div / 2;
        (*it)[1] = (*it)[1] / div * div + div / 2;
        (*it)[2] = (*it)[2] / div * div + div / 2;
    }

}

void sharpen2D(cv::Mat image, cv::Mat result)
{
    cv::Mat kernel = (cv::Mat_<float>(3, 3) << 0, -1, 0, -1, 5, -1, 0, -1, 0);
    cv::filter2D(image, result, image.depth(), kernel);
}
