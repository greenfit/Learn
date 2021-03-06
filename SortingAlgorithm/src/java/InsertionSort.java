import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * 插入排序.
 * 核心思想: 选择小的元素插入到前面.
 * 
 * @see https://heleeos.com/blog/sorting-algorithm-theory.html#3插入排序
 * @author liyu
 */
public class InsertionSort {

    public static void main(String[] args) {
        List<Integer> data = getData(20);
        System.err.println("数据:" + data);
        insertionSort(data);
        System.err.println("结果:" + data);
    }
    
    /**
     * 生成一组数据
     */
    public static List<Integer> getData(int n) {
        List<Integer> list = new ArrayList<>(n);
        for(int i = 0; i < n; i++)
            list.add(i, i);
        Collections.shuffle(list);//打乱顺序
        return list;
    }
    
    /**
     * 插入排序
     */
    public static void insertionSort(List<Integer> data) {
        int size = data.size();
        for(int i = 1; i < size; i++) {
            int number = data.get(i);
            
            //把当前的元素插入到已排序元素的对应位置
            for(int index = i - 1; index >= 0; index--) {
                if(number < data.get(index)) {
                    data.set(index + 1, data.get(index));
                    if(index == 0) {
                        data.set(index, number);
                    }
                } else {
                    data.set(index + 1, number);
                    break;
                }
            }
            
            System.out.printf("第 %s 次:", i);
            data.forEach(num -> System.out.printf("%s ", num));
            System.out.println();
        }
    }
}
