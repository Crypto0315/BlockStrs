package utils;

import entity.Polynomial;
import entity.PolynomialDivisionResult;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class StrsUtils {
    /**
     * 随机生成多项式  取值范围[-q,q]
     *
     * @param minValue
     * @param maxValue
     * @return
     */
    public static Long[] generateRandomLongArray(Integer l, Long minValue, Long maxValue) {
        // 创建一个 Random 对象
        Random random = new Random();

        Long[] randomArray = new Long[l];

        for (int i = 0; i < l; i++) {
            // 生成一个介于 minValue 和 maxValue 之间的随机长整数
            long range =  maxValue -  minValue + 1;
            long randomValue = minValue + (long) (random.nextDouble() * range);
            randomArray[i] = randomValue;
        }

        return randomArray;
    }


    /**
     * 定义模多项式f
     *
     * @param n
     * @return
     */
    public static Long[] generateF(int n) {
        Long[] f = new Long[n+1];
        f[0] = 1L;
        f[(n)] = 1L;
        // 将数组中为null的元素设置为0
        for (int i = 1; i <= n - 1; i++) {
            if (f[i] == null) {
                f[i] = 0L;
            }
        }
        return f;
    }





    /**
     * 多项式乘法
     *
     * @param a
     * @param b
     * @return
     */
    public static Long[] convolution(Long[] a, Long[] b) {
        int m = a.length;
        int n = b.length;
        int resultLength = m + n - 1;
        Long[] result = new Long[resultLength];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (a[i] != null && b[j] != null) {
                    if (result[i + j] == null) {
                        result[i + j] = 0L;
                    }
                    result[i + j] += a[i] * b[j];
                }
            }
        }

        return result;
    }


    /**
     * 多项式除法
     * @param aa  数组
     * @param bb  数组
     * @param q   要mod的数  传0就不mod
     * @return
     */
    public static PolynomialDivisionResult deconv(Long[] aa, Long[] bb, Long q) {
        Polynomial a1 = new Polynomial(aa);
        Polynomial b1 = new Polynomial(bb);

        Polynomial p;
        int lengtha, lengthb;
        Long[] ma, mb;
        Long[] a; // 清除a1前面的0
        Long[] b; // 清除b1前面的0

        // Find the first non-zero coefficient in a1
        int i;
        for (i = 0; i < a1.getCoefficients().length; i++) {
            if (a1.getCoefficients()[i] != 0L) {
                break;
            }
        }
        lengtha = a1.getCoefficients().length - i;
        a = Arrays.copyOfRange(a1.getCoefficients(), i, a1.getCoefficients().length);

        // Find the first non-zero coefficient in b1
        for (i = 0; i < b1.getCoefficients().length; i++) {
            if (b1.getCoefficients()[i] != 0L) {
                break;
            }
        }
        lengthb = b1.getCoefficients().length - i;
        b = Arrays.copyOfRange(b1.getCoefficients(), i, b1.getCoefficients().length);

        p = new Polynomial(new Long[lengtha + 1 - lengthb]);
        ma = new Long[lengtha];
        mb = new Long[lengtha];

        // Initialize ma with a
        for (i = 0; i < lengtha; i++) {
            ma[i] = a[i];
        }

        for (i = 0; i < p.getCoefficients().length; i++) {
            p.getCoefficients()[i] = ma[i] / b[0];

            // Compute mb
            for (int j = 0; j < lengthb; j++) {
                mb[j] = p.getCoefficients()[i] * b[j];
            }

            // Update ma
            for (int j = i; j < (i + lengthb); j++) {
                ma[j] -= mb[j - i];
            }

            // 如果 q 不为 0，则对商进行模运算
            if (q != 0L) {
                p.getCoefficients()[i] = (p.getCoefficients()[i] % q + q) % q;
            }
        }

        // 在余数前面补零以保持长度相同
        int lengthDiff = aa.length - ma.length;
        if (lengthDiff > 0) {
            Long[] zeroPadding = new Long[lengthDiff];
            Arrays.fill(zeroPadding, 0L);
            ma = ArrayUtils.addAll(zeroPadding, ma);
        }

        return new PolynomialDivisionResult(p.getCoefficients(), ma);

    }

    /**
     *取余数的后n/2位
     * @param R
     * @param n
     * @param q
     * @return
     */
    public static Long[] mod(Long[] R, int n, long q) {
        int startIndex = n - 1; // 开始索引，从0开始计数，所以需要减1
        int endIndex = 2 * n - 2; // 结束索引

        Long[] result = new Long[n];
        int j = 0;

        for (int i = startIndex; i <= endIndex; i++) {
            if (Objects.equals(0L,q)){
                result[j] = R[i];
            }else {
                result[j] = (R[i] % q + q) % q;
            }

            j++;
        }

        return result;
    }



    /**
     * 多项式相加 模q
     *
     * @param polynomial1
     * @param polynomial2
     * @return
     */
    public static Long[] addPolynomials(Long[] polynomial1, Long[] polynomial2, Long q) {
        int n = Math.max(polynomial1.length, polynomial2.length);
        Long[] result = new Long[n];

        for (int i = 0; i < n; i++) {
            Long coeff1 = (i < polynomial1.length) ? polynomial1[i] : 0;
            Long coeff2 = (i < polynomial2.length) ? polynomial2[i] : 0;
            if (Objects.equals(0L, q)) {
                result[i] = (coeff1 + coeff2);
            } else {
                result[i] =  ((coeff1 + coeff2) % q + q) % q;
            }
        }

        return result;
    }

    public static long generateRandomLong(long minValue, long maxValue) {
        Random random = new Random();
        long range =  maxValue -  minValue + 1;
        long randomValue = minValue + (long) (random.nextDouble() * range);
        return randomValue;
    }


    /**
     * 多项式减法 模q
     *
     * @param polynomial1
     * @param polynomial2
     * @param q
     * @return
     */
    public static Long[] subtractPolynomialsModQ(Long[] polynomial1, Long[] polynomial2, long q) {
        int n = Math.max(polynomial1.length, polynomial2.length);
        Long[] result = new Long[n];

        for (int i = 0; i < n; i++) {
            Long coeff1 = (i < polynomial1.length) ? polynomial1[i] : 0;
            Long coeff2 = (i < polynomial2.length) ? polynomial2[i] : 0;

            // Perform the subtraction and take the result modulo q
            if (Objects.equals(0L, q)) {
                result[i] = coeff1 - coeff2;
            } else {
                Long subtractionResult = coeff1 - coeff2;
                result[i] = (subtractionResult % q + q) % q;
            }

        }

        return result;
    }


    /**
     生成 ti 数组，该数组包含从 t1 到 tN 的多项式，其中每个多项式都是通过将 t0 增加 i  theta 后取模 q 得到的
     * @param N
     * @param n
     * @param q
     * @param t0
     * @param theta
     * @return
     */
    public static Long[][] generateTiArray(int N, int n, long q, Long[] t0, Long[] theta) {
        Long[][] ti = new Long[N - 1][n];

        for (int i = 2; i <= N; i++) {
            for (int j = 0; j < n; j++) {
                // 如果 q 不等于 0，则进行 mod 操作；否则，直接赋值
                ti[i - 2][j] = (q != 0) ? ((t0[j] + i*theta[j]) % q + q) % q : (t0[j] + i*theta[j]);
            }
        }

        return ti;
    }


    /**
     *二维矩阵 每行当成多项式相乘，每列是多项式的系数
     * @param a 二维数组
     * @param b 二维数组
     * @return
     */
    /**
     * 二维矩阵每行当成多项式相乘，每列是多项式的系数
     * @param a 二维数组
     * @param b 二维数组
     * @return
     */
    public static Long[][] conv2(Long[][] a, Long[][] b) {
        // 获取输入矩阵和卷积核的行数和列数
        int numRowsA = a.length;
        int numColsA = a[0].length;
        int numRowsB = b.length;
        int numColsB = b[0].length;

        // 计算卷积后的结果矩阵的行数和列数
        int numRowsC = numRowsA;
        int numColsC = numColsA + numColsB - 1;

        // 创建结果矩阵
        Long[][] c = new Long[numRowsC][numColsC];

        // 执行多项式乘法运算
        for (int i = 0; i < numRowsA; i++) {
            for (int j = 0; j < numColsC; j++) {
                Long sum = 0L;
                for (int k = 0; k < numColsB; k++) {
                    if (j - k >= 0 && j - k < numColsA) {
                        sum += a[i][j - k] * b[i][k];
                    }
                }
                c[i][j] = sum;
            }
        }

        return c;
    }


    /**
     * 计算矩阵每一列的累加和
     * @param matrix 二维数组
     * @return 一维数组，包含每一列的累加和
     */
    public static Long[] sumColumns(Long[][] matrix) {
        int numRows = matrix.length;
        int numCols = matrix[0].length;
        Long[] columnSum = new Long[numCols];

        for (int j = 0; j < numCols; j++) {
            Long sum = 0L;
            for (int i = 0; i < numRows; i++) {
                sum += matrix[i][j];
            }
            columnSum[j] = sum;
        }

        return columnSum;
    }

    /**
     * 将一维数组的数据添加到二维数组中，使行数增加
     * @param arr2D 二维数组
     * @param arr1D 一维数组
     * @return 合并后的新二维数组
     */
    public static Long[][] mergeArrays(Long[][] arr2D, Long[]... arr1D) {
        int numRows = arr2D.length;
        int numCols = arr2D[0].length;
        int numToAdd = arr1D.length;

        // 创建新的二维数组，行数为原二维数组的行数加上要添加的一维数组的数量，列数保持不变
        Long[][] mergedArray = new Long[numRows + numToAdd][numCols];

        // 将原二维数组的数据复制到新的二维数组中
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                mergedArray[i][j] = arr2D[i][j];
            }
        }

        // 将一维数组的数据添加到新的二维数组中
        for (int i = 0; i < numToAdd; i++) {
            for (int j = 0; j < numCols; j++) {
                mergedArray[numRows + i][j] = arr1D[i][j];
            }
        }

        return mergedArray;
    }


    // 将二维long数组展平为字节数组
    public static byte[] flattenArray(Long[][] array) {
        int rows = array.length;
        int cols = array[0].length;
        byte[] result = new byte[rows*cols*8]; // 一个long占8个字节

        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                long value = array[i][j];
                for (int k = 0; k < 8; k++) {
                    result[index++] = (byte) (value >>> (k * 8));
                }
            }
        }

        return result;
    }


    /**
     * % The result of the hash output (character vector) is transformed
     *         % into a 1*d vector c composed of its ASCII code, which needs to
     *         % be assigned to c after ASCII mod 3 first, and then change 2 to -1.
     * @param hash
     * @return
     *
     *
     * ca87f7dfc8e5f9a16b25611a93d3f903d2466db70f3c80eb71768fa780d245be2f2e9bb1424f4bc891a5c25dfec0df0bd5a3f8e04258b503175a91d018dfae2f
     */
    public static Long[] transformHash(String hash) {
        Long[] c = new Long[hash.length()];

        for (int i = 0; i < hash.length(); i++) {
            int asciiCode = (int) hash.charAt(i);

            // Perform unsigned ASCII mod 3
            int modResult = (asciiCode % 3 + 3) % 3;

            // Change 2 to -1
            if (modResult == 2) {
                modResult = -1;
            }

            c[i] = Long.valueOf(modResult);
        }

        return c;
    }


    public static Long[] transformAndModify(Long[] inputArray) {
        int d = inputArray.length;

        // 将 inputArray 中的每个元素执行无符号 mod 3 操作
        for (int i = 0; i < d; i++) {
            long inputValue = inputArray[i];
            long modResult = (inputValue % 3 + 3) % 3; // 执行无符号 mod 3 操作

            // 将结果为 2 的元素修改为 -1
            if (modResult == 2) {
                modResult = -1L; // 使用长整数值 -1L
            }

            inputArray[i] = modResult;
        }

        return inputArray;

    }


    public static Long[][] generateTiArrayAll(int N, int n, long q, Long[] t0, Long[] theta) {
        Long[][] ti = new Long[N][n];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < n; j++) {
                // 如果 q 不等于 0，则进行 mod 操作；否则，直接赋值  result[j] = ((t0[j] + (i+1)*theta[j]) % q + q) % q;

                ti[i][j] = (q != 0) ? ((t0[j] + (i+1)*theta[j]) % q + q) % q : (t0[j] + (i+1)*theta[j]);
            }
        }

        return ti;
    }


    public static boolean compareArrays(Long[] newc, Long[] cc) {
        // 获取两个数组的长度
        int newcLength = newc.length;
        int ccLength = cc.length;

        // 比较数组的长度
        if (newcLength > ccLength) {
            // 如果newc的长度大于cc的长度，只比较cc长度的数据
            for (int i = 0; i < ccLength; i++) {
                if (!newc[i].equals(cc[i])) {
                    return false; // 有不相同的元素，返回false
                }
            }
            return true; // 所有元素都相同，返回true
        } else if (newcLength < ccLength) {
            // 如果newc的长度小于cc的长度，只比较newc长度的数据
            for (int i = 0; i < newcLength; i++) {
                if (!newc[i].equals(cc[i])) {
                    return false; // 有不相同的元素，返回false
                }
            }
            return true; // 所有元素都相同，返回true
        } else {
            // 如果两个数组长度相同，比较所有元素
            return Arrays.equals(newc, cc);
        }
    }

}



