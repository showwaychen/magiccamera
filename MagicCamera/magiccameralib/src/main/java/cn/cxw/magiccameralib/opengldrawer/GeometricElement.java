package cn.cxw.magiccameralib.opengldrawer;

import java.util.ArrayList;

/**
 * Created by cxw on 2018/1/1.
 */

public class GeometricElement {
    static String TAG = "GeometricElement";
    public static class Point
    {
        public static float distance(Point p1, Point p2)
        {
            int x = p1.x - p2.x;
            int y = p1.y - p2.y;
            return (float) Math.sqrt(x * x + y * y);
        }
        public Size canvasSize = null;
        public  Size getCanvasSize()
        {
            if (canvasSize == null)
            {
                return null;
            }
            return new Size(canvasSize.width, canvasSize.height);
        }
        public static void swapPoint(Point p1, Point p2)
        {
            int tmpx = p1.x;
            int tmpy = p1.y;
            p2.reset(p1);
            p1.reset(tmpx, tmpy);
        }
        public int x = 0;
        public int y = 0;
        public Point(){}
        public Point(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
        public void reset(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
        public void reset(Point point)
        {
            x = point.x;
            y = point.y;
        }
        public void reset(Size size)
        {
            float hratio = (float) size.height / (float)canvasSize.height;
            float wratio = (float)size.width / (float)canvasSize.width;
            canvasSize.width = size.width;
            canvasSize.height = size.height;
            x = (int) (x * wratio);
            y = (int) (y * hratio);
        }
        public double length()
        {
            return Math.sqrt(x * x + y * y);
        }
        public void translate(int xdela, int ydela)
        {
            x += xdela;
            y += ydela;
        }
        public void scale(float scaleratio)
        {
            x = (int) (x * scaleratio);
            y = (int) (y * scaleratio);
        }
        public void rotate(float degrees)
        {
            degrees = 360 - degrees;
            float radian = (float) (degrees * Math.PI / 180);
            int origx = x;
            int origy = y;
            x = (int) ((float) (origx * Math.cos(  radian)) - (float) origy * Math.sin(radian));
            y = (int) ((float)(origy * Math.cos(radian)) + (float)(origx * Math.sin(radian)));
        }
        public void setCanvasSize(Size size)
        {
            canvasSize = new Size(size.width, size.height);
        }
        public void convert2GlVexter(float[] vexter, int offset, int width, int height)
        {
            if (canvasSize != null)
            {
                float wratio = (float) width / (float) canvasSize.width;
                float hratio = (float) height / (float) canvasSize.height;
                    int newx = (int) (this.x * wratio);
                    int newy = (int) (this.y * hratio);
                    reset(newx, newy);
            }
            //最后转换成指定的opengl es 的坐标。
                vexter[offset] = (float)(x * 2) / width -1f;
                vexter[offset + 1 ] = (float)(y * 2) / height -1f;
                //再沿x轴翻转180度。
                vexter[offset + 1] *= -1;
        }
    }

    public static class Size
    {
        public int width = 0;
        public int height = 0;
        public Size(int width, int height)
        {
            this.width = width;
            this.height = height;
        }
    }
    public static class Line
    {
        Point point1 = null;
        Point point2 = null;
        public Line(Point p1, Point p2)
        {
            point1 = new Point(p1.x, p1.y);
            point1.setCanvasSize(p1.canvasSize);
            point2 = new Point(p2.x, p2.y);
            point2.setCanvasSize(p2.canvasSize);

        }

        //求过点垂直于此线段，相交于此线段的点。
        public Point perpendicularThroughPoint(Point point, int width, int height)
        {
            Size csize = point1.canvasSize;
            float wratio = (float) width / csize.width;
            float hratio = (float) height / csize.height;
            Point retpoint = new Point();
//            retpoint.setCanvasSize(new Size(width, height));
            retpoint.setCanvasSize(csize);

            float x = 0f;
            float y = 0f;
            if(point1.y == point2.y)
            {
                x = point.x;
                y = point2.y;
            }
            else
            {
//                float p1x = point1.x * wratio;
//                float p1y = point1.y * hratio;
//                float p2x = point2.x* wratio;
//                float p2y = point2.y * hratio;
                float p1x = point1.x;
                float p1y = point1.y;
                float p2x = point2.x;
                float p2y = point2.y;
                float k1 = 0f, b1 = 0f;
                //先求出直线的方程公式。
                k1 = (float) (p1y - p2y) / (float)(p1x - p2x);
                b1 = p1y - p1x * k1;

//                float px = point.x * wratio;
//                float py = point.y * hratio;
                float px = point.x ;
                float py = point.y ;
                //由于是垂直，所以斜率为直线的倒数的负数。
                float b2 = py + (float)px / k1;
                //最后通知交点公式计算出交点的坐标。
                x = k1*(b2 - b1)/( k1 * k1 + 1);
                y = k1 * x + b1;
//                Log.d(TAG, "" + wratio + "  " + hratio + " k1 = " + k1 + " b1 = " + b1 + "  b2 = " + b2 + " -1/k1 = " + ( -1f / k1));
//                Log.d(TAG, "p1 x = " + p1x + " y = " + p1y + "  p2 :x =" + p2x + " y = " + p2y + " p: x= " + px + "  y = " + py);
//                Log.d(TAG, " np : x = " + x + " y = " + y);
            }

            retpoint.reset((int) x, (int) y);
            return retpoint;
        }
    }
    public static class RotationRect
    {
        Size canvasSize = null;
        Point rotatePoint = null;
        Point lt = new Point();
        Point rt = new Point();
        Point rb = new Point();
        Point lb = new Point();
        ArrayList<Point> plist = new ArrayList<>();

        float degrees = 0f;
        public RotationRect(Rect rect, float degrees)
        {
            lt.reset(rect.left, rect.top);
            rt.reset(rect.right, rect.top);
            rb.reset(rect.right, rect.bottom);
            lb.reset(rect.left, rect.bottom);
            plist.add(lt);
            plist.add(rt);
            plist.add(rb);
            plist.add(lb);
            this.degrees = degrees;
        }
        public void setRotatePoint(Point rp)
        {
            rotatePoint = new Point(rp.x, rp.y);
        }
        void rotation(float degrees)
        {
            int centerX = 0;
            int centerY = 0;
            if (rotatePoint == null)
            {
                centerX = (lt.x + rt.x)/ 2;
                centerY = (lt.y + lb.y)/ 2;
            }
            else
            {
                centerX = rotatePoint.x;
                centerY = rotatePoint.y;
            }

            //先平移到原点.
            for (Point point:plist
                 ) {
                point.translate(-centerX, -centerY);
            }

            //绕原点旋转指定角度.
            for (Point point:plist
                    ) {
                point.rotate(degrees);
            }

            //移回原来的中心点。
            for (Point point:plist
                    ) {
                point.translate(centerX, centerY);
            }
        }

        public void setCanvasSize(Size size)
        {
            canvasSize = size;
        }
        public void  convert2GlVexter(float[] vexter, int stride, int width, int height)
        {
            //先拉伸到指定的大小。
            if (canvasSize != null)
            {
                float wratio = (float) width / (float) canvasSize.width;
                float hratio = (float) height / (float) canvasSize.height;
                for (Point point:plist
                     ) {
                    int newx = (int) (point.x * wratio);
                    int newy = (int) (point.y * hratio);
                    point.reset(newx, newy);
                }
            }
            //再旋转
            rotation(degrees);
            //最后转换成指定的opengl es 的坐标。
            for (int i = 0 ; i < 4; i++)
            {
                vexter[i * stride] = (float)(plist.get(i).x * 2) / width -1f;
                vexter[i * stride + 1 ] = (float)(plist.get(i).y * 2) / height -1f;
                //再沿x轴翻转180度。
                vexter[i * stride + 1] *= -1;
            }

        }
    }
    //坐标原点为左上角。
    public static class Rect
    {
        public String toString()
        {
            String str = new String();
            str = "lt: x=" + left + " y=" + top + " br: x=" + right + " y=" + bottom;
            return str;
        }
        public Size canvasSize = null;
        public int left = 0;
        public int top = 0;
        public int right = 0;
        public int bottom = 0;
        public Rect(int left, int top, int right, int bottom)
        {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
        public int width()
        {
            return right - left;
        }
        public int height()
        {
            return top - bottom;
        }
        public void translateRect(int xdela, int ydela)
        {
            left += xdela;
            right += xdela;
            top +=ydela;
            bottom += ydela;

        }
        public void setCanvasSize(Size size)
        {
            canvasSize = size;
        }
        public RotationRect rotationRect(float degrees)
        {
            RotationRect tmp = new RotationRect(this, degrees);
            tmp.setCanvasSize(canvasSize);
           return tmp;
        }
    }
}
