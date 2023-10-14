package ZJChain;

import utils.StringUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import static ZJChain.Wallet.*;
import static utils.StringUtil.bytesToHex;
import static utils.StrsUtils.*;

public class Transaction {
    /**
     * 交易号
     */
    public String transactionId;
    /**
     * 交易序号，用于记录交易数量
     */
    public static int sequence = 0;

    /**
     * 发送方的地址/public key
     */
    public Long[] strsSender;
    /**
     * 接收方的地址/public key
     */
    public Long[] strsrecipient;
    /**
     * 交易额
     */
    public float value;

    //C, z1, z2, theta, t0, h0
    public Long[] Z1;
    public Long[] Z2;
    public Long[] theTa;
    public Long[] T0;
    public Long[] H0;
    public Long[][] CI;

    public Long[] newC;


    /**
     * 本次交易所涉及到的所有交易输入
     */
    public ArrayList<TransactionInput> inputs = new ArrayList<>();
    /**
     * 本次交易所涉及到的所有交易输出（第0位output是发给别人的，第1位output是发给自己的）
     */
    public ArrayList<TransactionOutput> outputs = new ArrayList<>();

    /**
     * 发送方的钱包类
     */
    public Wallet wallet = new Wallet();


    public Transaction(Long[] from, Long[] to, float value, ArrayList<TransactionInput> inputs,Wallet wallet) {
        this.strsSender = from;
        this.strsrecipient = to;
        this.value = value;
        this.inputs = inputs;
        this.wallet = wallet;
    }

    /**
     * 计算用于标识交易的transactionId
     * @return
     * @throws Exception
     */
    private String calculateHash() throws Exception {
        sequence++;
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(strsSender) +
                        StringUtil.getStringFromKey(strsrecipient) +
                        value + sequence);
    }


    /**
     * 根据私钥和其它参数生成数字签名
     * @param n
     * @param q
     * @param h
     * @param f
     * @param PK
     * @param Lpk
     * @param miu
     * @param sk1
     * @param sk2
     */
    public void generateSignature(Integer n,Long q,Long[] h,Long[] f,Long[][] PK,Long[][] Lpk,Long[] miu, Long[] sk1,Long[] sk2) {
        //3.签名算法
        //计算tag  Compute t0 = H2(T,M), h0 = H2(T)，消息miu
        Long[] t0 = generateRandomLongArray(n, -1L, 1L);
        Long[] h0 = generateRandomLongArray(n, -1L, 1L);

        //计算tpai = sk1 +sk2*h0
        Long[] sk2h0 = convolution(sk2, h0);

        Long[] s2h0Remainder = deconv(sk2h0, f, q).getRemainder();
        Long[] s2h0 = mod(s2h0Remainder, n, q);

        Long[] tpai = addPolynomials(sk1, s2h0, q);
        Long[] tag = subtractPolynomialsModQ(tpai, t0, q);


        //计算ti = t0 + i*tag 其中i属于【2，N】
        Long[][] ti = generateTiArray(N, n, q, t0, tag);



        //生成多项式y1, y2
        Long[] y1 = generateRandomLongArray(n, -11500L, 11500L);
        Long[] y2 = generateRandomLongArray(n, -11500L, 11500L);

        //C = randi([-1,1], N-1, n);
        Long[][] C = new Long[N - 1][n];
        for (int i = 0; i < N - 1; i++) {
            for (int j = 0; j < n; j++) {
                C[i][j] = generateRandomLong(-1, 1);
            }
        }

        //Compute R0 = y1 + y2*h - c·Σpk
        Long[][] cipk = conv2(C, Lpk);
        Long[] y2h = convolution(y2, h);
        Long[] cpk = sumColumns(cipk);

        Long[] y2hcpk = subtractPolynomialsModQ(y2h, cpk, q);

        Long[] hcRemainder = deconv(y2hcpk, f, q).getRemainder();
        Long[] hc = mod(hcRemainder, n, q);

        Long[] R0 = addPolynomials(y1, hc, q);

        //Compute R1 = y1 + y2*h0 - c·Σt
        Long[][] citi = conv2(C, ti);
        Long[] y2h0 = convolution(y2, h0);
        Long[] cti = sumColumns(citi);

        Long[] y2h0cti = subtractPolynomialsModQ(y2h0, cti, q);
        Long[] h0cRemainder = deconv(y2h0cti, f, q).getRemainder();
        Long[] h0c = mod(h0cRemainder, n, q);

        Long[] R1 = addPolynomials(y1, h0c, q);



        //拼接PK  miu  R_0  R_1  tag
        //计算c = H(Lpk, μ, R0, R1, theta), c是128位
        Long[][] hash = mergeArrays(PK, miu, R0, R1, tag);
        Long[] c = new Long[128];
        try {
            // 创建SHA-256哈希算法的MessageDigest对象
            MessageDigest md = MessageDigest.getInstance("SHA-512");

            // 将数组的内容转换为字节数组
            byte[] arrayBytes = flattenArray(hash);

            // 计算哈希值
            byte[] hashBytes = md.digest(arrayBytes);

            // 将哈希值打印为十六进制字符串
            String hashHex = bytesToHex(hashBytes);

            c = transformHash(hashHex);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        //Compute cπ = c - Σci, i∈[N]\{π}.
        Long[] ci = sumColumns(C);
        Long[] cπ1 = subtractPolynomialsModQ(c, ci, 0L);
        Long[] cπ = transformAndModify(cπ1);

        Long[][] Ci = new Long[N][n];
        Ci[0] = cπ;//签名者本身的公钥


        // 将 ci 的内容复制到新数组的后续行 i∈[N]\{π}.
        for (int i = 1; i < N; i++) {
            Ci[i] = Arrays.copyOf(C[i - 1], n);
        }



        Long[] CiSum = sumColumns(Ci);

        //在这里需要mod 3
        Long[] newc = transformAndModify(CiSum);


        //计算z1  z2
        Long[] cπsk1 = convolution(cπ, sk1);
        Long[] csk1Remainder = deconv(cπsk1, f, q).getRemainder();
        Long[] csk1 = mod(csk1Remainder, n, q);
        Long[] z1 = addPolynomials(y1, csk1, q);

        Long[] cπsk2 = convolution(cπ, sk2);
        Long[] csk2Remainder = deconv(cπsk2, f, q).getRemainder();
        Long[] csk2 = mod(csk2Remainder, n, q);
        Long[] z2 = addPolynomials(y2, csk2, q);
        //System.out.println("签名算法的newC的结果是："+Arrays.asList(newc));

        //C, z1, z2, theta, t0, h0
        theTa=tag;
        Z1=z1;
        Z2=z2;
        T0=t0;
        H0=h0;
        CI=Ci;
        newC=newc;

    }



    /**
     * 检查发送方数字签名，以验证数据没有损坏或者被修改
     * @return
     */
    public boolean verifySignature(Wallet wallet) throws Exception {
        //function [result] = verify(n, q, h, f, Lpk, miu, C, z1, z2, theta, t0, h0)
        //3.验证算法
        //计算ti = t0 + i*tag i∈【1，N】
        Long[][] tii = generateTiArrayAll(N, n, q, T0, theTa);

        //计算R0
        Long[][] Cipki = conv2(CI, wallet.PKS);
        Long[] CPK = sumColumns(Cipki);
        Long[] z2h = convolution(Z2, wallet.H);

        Long[] z2hCPK = subtractPolynomialsModQ(z2h, CPK, q);
        Long[] hCRemainder = deconv(z2hCPK, wallet.F, q).getRemainder();
        Long[] hC = mod(hCRemainder, n, q);
        Long[] R_0 = addPolynomials(Z1, hC, q);

        //计算R1
        Long[][] Citii = conv2(CI, tii);
        Long[] Cti = sumColumns(Citii);
        Long[] z2h0 = convolution(Z2, H0);

        Long[] z2h0Cti = subtractPolynomialsModQ(z2h0, Cti, q);
        Long[] h0CRemainder = deconv(z2h0Cti, wallet.F, q).getRemainder();
        Long[] h0C = mod(h0CRemainder, n, q);
        Long[] R_1 = addPolynomials(Z1, h0C, q);


        //计算c = H(Lpk, μ, R0, R1, theta), c是128位
        Long[][] hash_ = mergeArrays(wallet.PKS, wallet.mius, R_0 , R_1, theTa);
        Long[] cc = new Long[128];
        try {
            // 创建SHA-256哈希算法的MessageDigest对象
            MessageDigest md = MessageDigest.getInstance("SHA-512");

            // 将数组的内容转换为字节数组
            byte[] arrayBytes = flattenArray(hash_);

            // 计算哈希值
            byte[] hashBytes = md.digest(arrayBytes);

            // 将哈希值打印为十六进制字符串
            String hashHex = bytesToHex(hashBytes);
            //System.out.print("cc的hash"+hashHex);

            //System.out.println("SHA-512 Hash: " + hashHex);
            cc = transformHash(hashHex);
            for (long value : cc) {
                //System.out.print(value + " ");
            }
            //System.out.println();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        boolean b = compareArrays(newC, cc);
        System.out.println("验证通过了!哈哈哈："+b);
        return b;
    }

    /**
     * 实现一次交易
     * @return
     */
    public boolean processTransaction(Wallet wallet) {

        //验证交易的发送方的数字签名是否有效
        try {
            if(!verifySignature(wallet)) {
                System.out.println("交易签名验证失败");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //根据交易输出的id从整个区块链中有效的UTXO集合中获取对应的UTXO
        for(TransactionInput input : inputs) {
            input.UTXO = ZJChain.UTXOs.get(input.transactionOutputId);
        }

        //检测交易输入额是否符合最小标准
        if(getInputsValue() < ZJChain.minimumTransaction) {
            System.out.println("交易输入数额：" + getInputsValue() + " 小于最小交易额");
            return false;
        }

        //计算交易输入还有多少剩余（类似找零）
        float leftover = getInputsValue() - value;
        if(leftover < 0) {
            System.out.println("金额不足，交易终止！");
            return false;
        }
        //计算交易id
        try {
            transactionId = calculateHash();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //建立指向收款方的交易输出
        outputs.add(new TransactionOutput(this.strsrecipient, value, transactionId));
        //如果需要找零才找零
        if(leftover > 0) {
            //建立指向发送方的交易输出（将交易输出中没有用完的还给自己，实现找零功能）
            outputs.add(new TransactionOutput(this.strsSender, leftover, transactionId));
        }

        //将本次交易中的所有交易输出添加到整个区块链的UTXO集合中（实现向所有用户通报这笔交易）
        for(TransactionOutput output : outputs) {
            ZJChain.UTXOs.put(output.id, output);
        }
        //移除整个区块链中本次交易中所有交易输入所对应的UTXO（每个UTXO只能用来支付一次）
        for(TransactionInput input : inputs) {
            if(input.UTXO != null) {
                ZJChain.UTXOs.remove(input.UTXO.id);
            }
        }

        return true;
    }

    /**
     * 获取所有交易输入中的总价值（计算拥有的钱的总数）
     * @return
     */
    public float getInputsValue() {
        float sum = 0;
        for(TransactionInput i : inputs) {
            if(i.UTXO != null) {
                sum += i.UTXO.value;
            }
        }
        return sum;
    }

    /**
     * 获取所有交易输出中的总价值（要支付的钱的总数）
     * @return
     */
    public float getOutputsValue() {
        float sum = 0;
        for(TransactionOutput output : outputs) {
            sum += output.value;
        }
        return sum;
    }

}
