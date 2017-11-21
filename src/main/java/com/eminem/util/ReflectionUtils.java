package com.eminem.util;

/**
 * Created by 王欣宇 on 2017/11/21.
 */
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class ReflectionUtils {
    private static Logger logger = LoggerFactory
            .getLogger(ReflectionUtils.class);

    /**
     * 调用Getter方法.
     */
    public static Object invokeGetterMethod(Object obj, String propertyName) {
        String getterMethodName = "get" + StringUtils.capitalize(propertyName);
        return invokeMethod(obj, getterMethodName, new Class[] {},
                new Object[] {});
    }

    /**
     * 调用Setter方法.使用value的Class来查找Setter方法.
     */
    public static void invokeSetterMethod(Object obj, String propertyName,
                                          Object value) {
        invokeSetterMethod(obj, propertyName, value, null);
    }

    /**
     * 调用Setter方法.
     *
     * @param propertyType
     *            用于查找Setter方法,为空时使用value的Class替代.
     */
    public static void invokeSetterMethod(Object obj, String propertyName,
                                          Object value, Class<?> propertyType) {
        Class<?> type = propertyType != null ? propertyType : value.getClass();
        String setterMethodName = "set" + StringUtils.capitalize(propertyName);
        invokeMethod(obj, setterMethodName, new Class[] { type },
                new Object[] { value });
    }

    /**
     * 直接读取对象属性值, 无视private/protected修饰符, 不经过getter函数.
     */
    public static Object getFieldValue(final Object obj, final String fieldName) {
        Field field = getAccessibleField(obj, fieldName);

        if (field == null) {
            throw new IllegalArgumentException("Could not find field ["
                    + fieldName + "] on target [" + obj + "]");
        }

        Object result = null;
        try {
            result = field.get(obj);
        } catch (IllegalAccessException e) {
            logger.error("不可能抛出的异常{}", e.getMessage());
        }
        return result;
    }

    /**
     * 直接设置对象属性值, 无视private/protected修饰符, 不经过setter函数.
     */
    public static void setFieldValue(final Object obj, final String fieldName,
                                     final Object value) {
        Field field = getAccessibleField(obj, fieldName);

        if (field == null) {
            throw new IllegalArgumentException("Could not find field ["
                    + fieldName + "] on target [" + obj + "]");
        }

        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            logger.error("不可能抛出的异常:{}", e.getMessage());
        }
    }

    /**
     * 循环向上转型, 获取对象的DeclaredField, 并强制设置为可访问.
     *
     * 如向上转型到Object仍无法找到, 返回null.
     */
    public static Field getAccessibleField(final Object obj,
                                           final String fieldName) {
        Assert.notNull(obj, "object不能为空");
        Assert.hasText(fieldName, "fieldName");
        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass
                .getSuperclass()) {
            try {
                Field field = superClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {// NOSONAR
                // Field不在当前类定义,继续向上转型
            }
        }
        return null;
    }

    /**
     * 直接调用对象方法, 无视private/protected修饰符. 用于一次性调用的情况.
     */
    public static Object invokeMethod(final Object obj,
                                      final String methodName, final Class<?>[] parameterTypes,
                                      final Object[] args) {
        Method method = getAccessibleMethod(obj, methodName, parameterTypes);
        if (method == null) {
            throw new IllegalArgumentException("Could not find method ["
                    + methodName + "] on target [" + obj + "]");
        }

        try {
            return method.invoke(obj, args);
        } catch (Exception e) {
            throw convertReflectionExceptionToUnchecked(e);
        }
    }

    /**
     * 循环向上转型, 获取对象的DeclaredMethod,并强制设置为可访问. 如向上转型到Object仍无法找到, 返回null.
     *
     * 用于方法需要被多次调用的情况. 先使用本函数先取得Method,然后调用Method.invoke(Object obj, Object...
     * args)
     */
    public static Method getAccessibleMethod(final Object obj,
                                             final String methodName, final Class<?>... parameterTypes) {
        Assert.notNull(obj, "object不能为空");

        for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass
                .getSuperclass()) {
            try {
                Method method = superClass.getDeclaredMethod(methodName,
                        parameterTypes);

                method.setAccessible(true);

                return method;

            } catch (NoSuchMethodException e) {// NOSONAR
                // Method不在当前类定义,继续向上转型
            }
        }
        return null;
    }

    /**
     * 通过反射, 获得Class定义中声明的父类的泛型参数的类型. 如无法找到, 返回Object.class. eg. public UserDao
     * extends HibernateDao<User>
     *
     * @param clazz
     *            The class to introspect
     * @return the first generic declaration, or Object.class if cannot be
     *         determined
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Class<T> getSuperClassGenricType(final Class clazz) {
        return getSuperClassGenricType(clazz, 0);
    }

    /**
     * 通过反射, 获得Class定义中声明的父类的泛型参数的类型. 如无法找到, 返回Object.class.
     *
     * 如public UserDao extends HibernateDao<User,Long>
     *
     * @param clazz
     *            clazz The class to introspect
     * @param index
     *            the Index of the generic ddeclaration,start from 0.
     * @return the index generic declaration, or Object.class if cannot be
     *         determined
     */
    @SuppressWarnings("rawtypes")
    public static Class getSuperClassGenricType(final Class clazz,
                                                final int index) {

        Type genType = clazz.getGenericSuperclass();

        if (!(genType instanceof ParameterizedType)) {
            logger.warn(clazz.getSimpleName()
                    + "'s superclass not ParameterizedType");
            return Object.class;
        }

        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();

        if (index >= params.length || index < 0) {
            logger.warn("Index: " + index + ", Size of "
                    + clazz.getSimpleName() + "'s Parameterized Type: "
                    + params.length);
            return Object.class;
        }
        if (!(params[index] instanceof Class)) {
            logger.warn(clazz.getSimpleName()
                    + " not set the actual class on superclass generic parameter");
            return Object.class;
        }

        return (Class) params[index];
    }

    /**
     * 将反射时的checked exception转换为unchecked exception.
     */
    public static RuntimeException convertReflectionExceptionToUnchecked(
            Exception e) {
        if (e instanceof IllegalAccessException
                || e instanceof IllegalArgumentException
                || e instanceof NoSuchMethodException) {
            return new IllegalArgumentException("Reflection Exception.", e);
        } else if (e instanceof InvocationTargetException) {
            return new RuntimeException("Reflection Exception.",
                    ((InvocationTargetException) e).getTargetException());
        } else if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        }
        return new RuntimeException("Unexpected Checked Exception.", e);
    }

    /**
     * 根据对象获得mongodb Update语句 除id字段以外，所有被赋值的字段都会成为修改项
     */
    public static Update getUpdateObj(final Object obj) {
        if (obj == null)
            return null;
        Field[] fields = obj.getClass().getDeclaredFields();
        Update update = null;
        boolean isFirst = true;
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value != null) {
                    if ("id".equals(field.getName().toLowerCase())
                            || "serialversionuid".equals(field.getName()
                            .toLowerCase()))
                        continue;
                    if (isFirst) {
                        update = Update.update(field.getName(), value);
                        isFirst = false;
                    } else {
                        update = update.set(field.getName(), value);
                    }
                }

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return update;
    }

    /**
     * 根据对象获得mongodb Query语句
     *
     * 1.时间范围查询：在时间字段前增加begin或end，为这两个字段分别赋值 例：private Date createDate; 开始时间
     * private Date beginCreateDate; 结束时间 private Date endCreateDate;
     * 分析后结果：where createDate >= beginCreateDate and createDate <
     * beginCreateDate
     *
     * 2.排序 定义并赋值VO中 orderBy 字段，以英文“,”分割多个排序，以空格分隔排序方向 asc可不写 例：private String
     * orderBy; orderBy="createDate desc,sendDate asc,id" 分析结构：order by
     * createDate desc,sendDate asc,id asc
     *
     * 3.固定值搜索 定义并赋值VO中的任意字段，搜索时会把以赋值的字段当作为搜索条件
     */

    public static Query getQueryObj(final Object obj) {
        if (obj == null)
            return null;
        Field[] fields = obj.getClass().getDeclaredFields();
        // Sort sort=new Sort(new Order(Direction.DESC,"createDate"));
        Query query = new Query();
        // 存放日期范围或者确定日期
        Map<String, Criteria> dateMap = new HashMap<String, Criteria>();
        String sortStr = null;
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value != null) {
                    if ("serialversionuid"
                            .equals(field.getName().toLowerCase())) {
                        continue;
                    }
                    if ("orderby".equals(field.getName().toLowerCase())) {
                        sortStr = String.valueOf(value);
                        continue;
                    }
                    if (field.getType().getSimpleName().equals("Date")) {
                        if (field.getName().toLowerCase().startsWith("begin")) {
                            String beginName = field.getName().substring(5);
                            if (beginName.isEmpty()) {
                                dateMap.put("begin", Criteria.where("begin")
                                        .is(value));
                            } else {
//                              beginName = StringUtil
//                                      .toLowerCaseFirstOne(beginName);
                                Criteria criteria = dateMap.get(beginName) == null ? Criteria
                                        .where(beginName).gte(value) : dateMap
                                        .get(beginName).gte(value);
                                dateMap.put(beginName, criteria);
                            }
                            continue;
                        }
                        if (field.getName().toLowerCase().startsWith("end")) {
                            String endName = field.getName().substring(3);
                            if (endName.isEmpty()) {
                                dateMap.put("end",
                                        Criteria.where("end").is(value));
                            } else {
//                              endName = StringUtil
//                                      .toLowerCaseFirstOne(endName);
                                Criteria criteria = dateMap.get(endName) == null ? Criteria
                                        .where(endName).lt(value) : dateMap
                                        .get(endName).lt(value);
                                dateMap.put(endName, criteria);
                            }
                            continue;
                        }
                        dateMap.put(field.getName(),
                                Criteria.where(field.getName()).is(value));
                        continue;
                    }
                    query.addCriteria(Criteria.where(field.getName()).is(value));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 日期类型查询条件
        for (String key : dateMap.keySet()) {
            if (dateMap.get(key) != null) {
                query.addCriteria(dateMap.get(key));
            }
        }
        // 排序
        if (sortStr != null && !sortStr.trim().isEmpty()) {
            Sort sort = null;
            String[] strs = sortStr.split(",");
            for (String str : strs) {
                str = str.trim();
                if (str.isEmpty()) {
                    continue;
                }
                int i = str.indexOf(" ");
                if (i < 0) {
                    if (sort == null) {
                        sort = new Sort(Direction.ASC, str);
                    } else {
                        sort = sort.and(new Sort(Direction.ASC, str));
                    }
                } else {
                    String name = str.substring(0, i);
                    String dire = str.substring(i + 1).trim();
                    Sort sn = null;
                    if ("desc".equals(dire.toLowerCase())) {
                        sn = new Sort(Direction.DESC, name);
                    } else {
                        sn = new Sort(Direction.ASC, name);
                    }
                    if (sort == null) {
                        sort = sn;
                    } else {
                        sort = sort.and(sn);
                    }
                }
            }
            if (sort != null) {
                query.with(sort);
            }
        }
        return query;
    }

}