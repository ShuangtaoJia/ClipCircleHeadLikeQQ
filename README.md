# ClipCircleHeadLikeQQ
仿QQ圆头像截取功能

效果图：

使用方法：



1.打开截图界面

    /**
     * 打开截图界面
     * @param uri 原图的Uri
     */
    public void starCropPhoto(Uri uri) {

        if (uri == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setClass(this, ClipHeaderActivity.class);
        intent.setData(uri);
        intent.putExtra("side_length", 200);//裁剪图片宽高
        startActivityForResult(intent, CROP_PHOTO);
    }
    

2.获取返回结果


在onActivityResult 方法中
Uri uri = intent.getData();

3.上传头像


可以通过uri得到截图文件的本地路径，可以方便的上传到服务器

4.头像展示


iv_head_icon.setImageURI(uri);  
ImageView 提供有直接设置图片URI的方法，可以方便的在ImageView中展示



5.具体的项目介绍请阅读我的博客

http://blog.csdn.net/taotao110120119/article/details/49229137
