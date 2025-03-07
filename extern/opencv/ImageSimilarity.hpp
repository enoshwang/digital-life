#include <opencv2/opencv.hpp>

#include <vector>
#include <string>

class ImageSimilarity {
public:
    // 构造函数，初始化 ORB 检测器和 BFMatcher
    ImageSimilarity();

    // 计算两张图像的相似度，返回 0 到 100 之间的相似度值
    double calculateSimilarity(const cv::Mat& img1, const cv::Mat& img2, const std::string matchesFilename = "") const;

private:
    // ORB 特征检测器和 BFMatcher
    cv::Ptr<cv::ORB> detector;
    cv::BFMatcher matcher;
};