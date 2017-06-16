package com.wangx.multithemelibrary;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wangx.multithemelibrary.parser.BaseParser;
import com.wangx.multithemelibrary.parser.ImageViewParser;
import com.wangx.multithemelibrary.parser.TextViewParser;

import java.lang.reflect.Field;

/**
 * @Author: xujie.wang
 * @Email: xujie.wang@17zuoye.com
 * @Date: 2017/5/26
 * @Project: MultiThemeLibrary
 */

public class LayoutInflaterFactoryDelegate implements android.support.v4.view.LayoutInflaterFactory {
    //    private List<View> parentViews = new ArrayList<>();
    private ViewGroup root;
    private TextViewParser mTextViewParser;
    private ImageViewParser mImageViewParser;
    private BaseParser mBaseParser;

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        View view = null;
        if (parent != null) {
            view = createView(name, context, attrs);
            if (view != null) {
                parseAttrs(name, context, attrs, view);
            }
        }
        return view;
    }

    /**
     * @param context
     */
    public void changeTheme(Context context) {
        if (root != null) {
            int childCount = root.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = root.getChildAt(i);
                BaseParser parser = (BaseParser) child.getTag();
                if (child instanceof TextView) {
                    TextViewParser textViewParser = (TextViewParser) parser;
                    TypedValue out = new TypedValue();
                    // textViewParser.resId  <- R.attr.xxx  <-?xxxxxxx 中的xxxx
                    context.getTheme().resolveAttribute(textViewParser.resId, out, true);
                    TextView textView = (TextView) child;
                    textView.setTextColor(context.getResources().getColor(out.resourceId));

                    context.getTheme().resolveAttribute(textViewParser.backgroundColor, out, true);
                    textView.setBackgroundColor(context.getResources().getColor(out.resourceId));
                } else if (child instanceof ImageView) {
                    ImageViewParser imageViewParser = (ImageViewParser) parser;
                    TypedValue out = new TypedValue();
                    ImageView imageView = (ImageView) child;
                    context.getTheme().resolveAttribute(imageViewParser.src, out, true);
                    imageView.setImageResource(out.resourceId);
                } else if (child instanceof View) {//最后匹配的类型
                    TypedValue out = new TypedValue();
                    context.getTheme().resolveAttribute(parser.backgroundColor, out, true);
                    child.setBackgroundColor(out.resourceId);
                }
            }
            //changeRoot
            changeRoot(context);
        }
    }

    private void changeRoot(Context context) {
        if (root instanceof View) {//最后匹配的类型
            BaseParser parser = (BaseParser) root.getTag();
            TypedValue out = new TypedValue();
            context.getTheme().resolveAttribute(parser.backgroundColor, out, true);
            root.setBackgroundColor(context.getColor(out.resourceId));
        }
    }

    /**
     * 解析属性 对 ?attr/对存储
     *
     * @param name    view的标签名
     * @param context
     * @param attrs
     * @param view    当前解析的view
     */
    private void parseAttrs(String name, Context context, AttributeSet attrs, View view) {
        //TODO 创建对应的Parser
        createParser(view);

        int attrCount = attrs.getAttributeCount();
        final Resources resources = context.getResources();
        for (int i = 0; i < attrCount; i++) {
            String attributeName = attrs.getAttributeName(i);
            String attributeValue = attrs.getAttributeValue(i);
            if ("id".equalsIgnoreCase(attributeName) && attributeValue.startsWith("@")) {
                // 处理有id的
                int id = Integer.parseInt(attributeValue.substring(1));
                try {
                    Field idField = R.id.class.getField("root");
                    idField.setAccessible(true);
                    int rootId = idField.getInt(null);
                    if (rootId == id) {
                        //root
                        root = (ViewGroup) view;
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if (isSupportAttr(attributeName)) {
                System.out.println("attributeName = " + attributeName + "::attributeValue=" + attributeValue);
                if (attributeValue.startsWith("?")) {
                    int resId = Integer.parseInt(attributeValue.substring(1));
                    String entryType = resources.getResourceTypeName(resId); //attr
                    String entryName = resources.getResourceEntryName(resId);//txt_hello_color

                    checkNull(entryType, "entryType can't be null");

                    // ?attr/txt_hello_color
                    TypedValue out = new TypedValue();
                    context.getTheme().resolveAttribute(resId, out, true);
                    createAttr(attributeName, out.resourceId, resId);

                }
            }
        }

    }

    private void createParser(View view) {
        if (view instanceof TextView) {
            mTextViewParser = new TextViewParser();
            view.setTag(mTextViewParser);
        } else if (view instanceof ImageView) {
            mImageViewParser = new ImageViewParser();
            view.setTag(mImageViewParser);
        } else if (view instanceof View) {
            mBaseParser = new BaseParser();
            view.setTag(mBaseParser);
        }
    }

    private void createAttr(String attributeName, int resValue, int resId) {
        if ("background".equalsIgnoreCase(attributeName)) {
            if (mTextViewParser != null) {
                mTextViewParser.backgroundColor = resId;

            } else if (mImageViewParser != null) {
                mImageViewParser.backgroundColor = resId;
            }else if (mBaseParser!= null) {
                mBaseParser.backgroundColor = resId;
            }
        } else if ("textColor".equalsIgnoreCase(attributeName)) {
            mTextViewParser.resId = resId;

        } else if ("src".equalsIgnoreCase(attributeName)) {
            mImageViewParser.src = resId;
        }
    }

    private void checkNull(@Nullable String nonNull, String errorMessage) {
        if (nonNull == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * 是否支持 ?attr/ 设置
     *
     * @param attributeName
     * @return
     */
    private boolean isSupportAttr(String attributeName) {
        if ("background".equalsIgnoreCase(attributeName)
                || "textColor".equalsIgnoreCase(attributeName)
                || "src".equalsIgnoreCase(attributeName)) {
            return true;
        }
        return false;
    }

    /**
     * 创建view，目的解析自定义可解析属性?attr/ 值 存储起来
     *
     * @param name
     * @param context
     * @param attrs
     * @return
     */
    private View createView(String name, Context context, AttributeSet attrs) {
        View view;
        if (!name.contains(".")) {
            view = createViewFromTag(name, context, attrs);
        } else {
            // 自定义控件
            view = createView(name, context, attrs, null);
        }
        return view;
    }

    private View createViewFromTag(String name, Context context, AttributeSet attrs) {
        View view = null;
        if ("View".equalsIgnoreCase(name)) {
            view = createView(name, context, attrs, "android.view.");
        }
        if (view == null) {
            view = createView(name, context, attrs, "android.widget.");
        }
        if (view == null) {
            view = createView(name, context, attrs, "android.webkit.");
        }
        return view;
    }

    private View createView(String name, Context context, AttributeSet attrs, String prefix) {
        View view = null;
        try {
            view = LayoutInflater.from(context).createView(name, prefix, attrs);
        } catch (Exception e) {
//            e.printStackTrace();
            Log.e("throw exception", "createView: ", e);
        }
        return view;
    }
}
