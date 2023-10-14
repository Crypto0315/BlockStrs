package ZJChain;

import entity.PolynomialDivisionResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static utils.StrsUtils.*;

public class Wallet {

    //function [C, z1, z2, theta, t0, h0] = signature(n, q, h, f, Lpk, miu, s1, s2)
    //公私钥
    //public PublicKey publicKey;
    //public PrivateKey privateKey;


    //环成员公钥
    public Long[] strsPublicKey;
    //环成员私钥1
    public Long[] strsPrivateKey1;
    //环成员私钥2
    public Long[] strsPrivateKey2;

    public  Long[] H;

    public  Long[] F;

    //消息  miu
    public  Long[] mius;
    //环公钥   拼接环中人数的所有公钥
    public  Long[][] PKS;
    public Long[][] LPK;

    //初始化参数
    public static Integer n = 1024;
    //环成员个数
    public static final Integer N = 8;
    //初始化参数  大素数需要被模%使用
    public static Long q = (long) Math.pow(2, 26);



    /**
     * 钱包存储属于自己的UTXO（未消费交易输出）
     */
    public HashMap<String, TransactionOutput> UTXOs = new HashMap<>();

    public Wallet() {
        generateKeyPair();
    }

    /**
     * 生成公私钥
     */
    public void generateKeyPair() {
        try {
            //1.系统建立
            Long[] h = generateRandomLongArray(n, -q, q);
            //生成不可约多项式f(x)=x^(n-1)+1
            Long[] f = generateF(n);


            //2.密钥生成
            Long[] sk1 = generateRandomLongArray(n, -1L, 1L);
            Long[] sk2 = generateRandomLongArray(n, -1L, 1L);

            //计算pk = s1 + s2*h
            Long[] sk2h = convolution(sk2, h);
            PolynomialDivisionResult result = deconv(sk2h, f, q);
            //deconv 的商
            //Long[] s2h = result.getQuotient();
            //deconv 的余数
            Long[] s2hRemainder = result.getRemainder();
            //s2h = mod(R(1,n:2*n-1),q);
            Long[] s2h = mod(s2hRemainder, n, q);

            Long[] pk = addPolynomials(sk1, s2h, q);

            // 创建Lpk数组  Lpk = [PK;randi([-q,q], N-1, n)];  其他成员的公钥
            Long[][] Lpk = new Long[N - 1][n];
            // 填充剩余的四行
            for (int i = 0; i < N - 1; i++) {
                for (int j = 0; j < n; j++) {
                    Lpk[i][j] = generateRandomLong(-q, q);
                }
            }

            // 创建新的数组，将 pk 复制到新数组的第一行
            Long[][] PK = new Long[N][n];
            PK[0] = pk;//签名者本身的公钥

            // 将 Lpk 的内容复制到新数组的后续行
            for (int i = 1; i < N; i++) {
                PK[i] = Arrays.copyOf(Lpk[i - 1], n);
            }

            Long[] miu = generateRandomLongArray(n, -1L, 1L);

            strsPrivateKey1 = sk1;
            strsPrivateKey2 = sk2;
            strsPublicKey = pk;
            mius=miu;
            PKS=PK;
            H=h;
            F=f;
            LPK=Lpk;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 计算钱包的总余额
     * @return
     */
    public float getBalance() {
        float sum = 0;
        //遍历Map集合获取键值对对象
        for(Map.Entry<String, TransactionOutput> item : ZJChain.UTXOs.entrySet()) {
            TransactionOutput UTXO =  item.getValue();
            //检查该UTXO是否属于该钱包
            if(UTXO.isMine(strsPublicKey)) {
                //添加到钱包的UTXOs集合中
                UTXOs.put(UTXO.id, UTXO);
                sum += UTXO.value;
            }
        }
        return sum;
    }


    /**
     * 创建交易，支出
     * @param _recipient
     * @param value
     * @return
     */
    public Transaction sendFunds(Long[] _recipient, float value,Wallet wallet) {
        //检查余额是否足够
        if(getBalance() < value) {
            System.out.println("余额不足，交易终止！");
            return null;
        }
        //建立动态数组用来记录作为交易输入使用的UTXO
        ArrayList<TransactionInput> inputs = new ArrayList<>();

        //查找钱包的UTXO，直到总金额达到要支付的金额
        float total = 0;
        for(Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            total += UTXO.value;
            inputs.add(new TransactionInput(UTXO.id));
            if(total >= value) {
                break;
            }
        }

        //创建交易
        Transaction newTransaction = new Transaction(strsPublicKey, _recipient, value, inputs,wallet);
        //newTransaction.generateSignature(privateKey);
        newTransaction.generateSignature(n,q,H,F,PKS,LPK,mius,strsPrivateKey1,strsPrivateKey2);

        //将已经使用的UTXO从钱包中移除
        for(TransactionInput input : inputs) {
            UTXOs.remove(input.transactionOutputId);
        }

        return  newTransaction;
    }

}
