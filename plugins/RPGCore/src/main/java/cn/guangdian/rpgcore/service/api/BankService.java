package cn.guangdian.rpgcore.service.api;

import cn.guangdian.bank.data.Loan;
import cn.guangdian.bank.data.TransactionRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 银行服务接口
 * 
 * <p>提供玩家银行账户管理、存取款、借贷、利息等完整银行功能。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取服务
 * BankService bank = serviceRegistry.getService(BankService.class);
 * 
 * // 存款
 * bank.deposit(playerId, 1000, "ATM存款");
 * 
 * // 申请贷款
 * Optional<Loan> loan = bank.applyForLoan(playerId, 5000, 30);
 * 
 * // 查询余额
 * long balance = bank.getBalance(playerId);
 * 
 * // 异步操作
 * bank.getBalanceAsync(playerId).thenAccept(bal -> {
 *     player.sendMessage("银行余额: " + bal);
 * });
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface BankService {

    // ==================== 账户管理 ====================

    /**
     * 获取玩家银行余额
     * 
     * @param playerId 玩家UUID
     * @return 银行余额
     */
    long getBalance(UUID playerId);

    /**
     * 检查玩家是否有银行账户
     * 
     * @param playerId 玩家UUID
     * @return 如果有账户返回 true
     */
    boolean hasAccount(UUID playerId);

    /**
     * 创建银行账户
     * 
     * @param playerId 玩家UUID
     * @return 如果创建成功返回 true
     */
    boolean createAccount(UUID playerId);

    /**
     * 获取玩家信用评分
     * 
     * @param playerId 玩家UUID
     * @return 信用评分 (0-100)
     */
    int getCreditScore(UUID playerId);

    /**
     * 设置玩家信用评分
     * 
     * @param playerId 玩家UUID
     * @param score 信用评分 (0-100)
     * @param reason 原因
     */
    void setCreditScore(UUID playerId, int score, String reason);

    // ==================== 存取款操作 ====================

    /**
     * 存款
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param reason 原因
     * @return 如果存款成功返回 true
     */
    boolean deposit(UUID playerId, long amount, String reason);

    /**
     * 取款
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param reason 原因
     * @return 如果取款成功返回 true
     */
    boolean withdraw(UUID playerId, long amount, String reason);

    /**
     * 转账
     * 
     * @param from 付款方UUID
     * @param to 收款方UUID
     * @param amount 金额
     * @param reason 原因
     * @return 如果转账成功返回 true
     */
    boolean transfer(UUID from, UUID to, long amount, String reason);

    /**
     * 检查是否有足够余额
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @return 如果余额足够返回 true
     */
    boolean hasBalance(UUID playerId, long amount);

    // ==================== 借贷操作 ====================

    /**
     * 申请贷款
     * 
     * @param playerId 玩家UUID
     * @param amount 贷款金额
     * @param durationDays 贷款期限（天）
     * @return 贷款对象（如果申请成功）
     */
    Optional<Loan> applyForLoan(UUID playerId, long amount, int durationDays);

    /**
     * 还款
     * 
     * @param playerId 玩家UUID
     * @param loanId 贷款ID
     * @param amount 还款金额
     * @return 如果还款成功返回 true
     */
    boolean repayLoan(UUID playerId, String loanId, long amount);

    /**
     * 获取玩家的所有活跃贷款
     * 
     * @param playerId 玩家UUID
     * @return 贷款列表
     */
    List<Loan> getActiveLoans(UUID playerId);

    /**
     * 获取贷款详情
     * 
     * @param loanId 贷款ID
     * @return 贷款对象
     */
    Optional<Loan> getLoan(String loanId);

    /**
     * 检查玩家是否可以申请贷款
     * 
     * @param playerId 玩家UUID
     * @param amount 贷款金额
     * @return 如果可以申请返回 true
     */
    boolean canApplyForLoan(UUID playerId, long amount);

    /**
     * 获取玩家可贷款的最大金额
     * 
     * @param playerId 玩家UUID
     * @return 最大可贷款金额
     */
    long getMaxLoanAmount(UUID playerId);

    // ==================== 利息操作 ====================

    /**
     * 计算并发放存款利息
     * 
     * @param playerId 玩家UUID
     * @return 发放的利息金额
     */
    long calculateAndPayInterest(UUID playerId);

    /**
     * 获取存款利率
     * 
     * @return 存款利率（百分比）
     */
    double getDepositInterestRate();

    /**
     * 获取贷款利率
     * 
     * @return 贷款利率（百分比）
     */
    double getLoanInterestRate();

    /**
     * 设置存款利率
     * 
     * @param rate 利率（百分比）
     */
    void setDepositInterestRate(double rate);

    /**
     * 设置贷款利率
     * 
     * @param rate 利率（百分比）
     */
    void setLoanInterestRate(double rate);

    // ==================== 交易记录 ====================

    /**
     * 获取交易历史
     * 
     * @param playerId 玩家UUID
     * @param limit 记录数量限制
     * @return 交易记录列表
     */
    List<TransactionRecord> getTransactionHistory(UUID playerId, int limit);

    /**
     * 获取所有交易记录
     * 
     * @param playerId 玩家UUID
     * @return 交易记录列表
     */
    List<TransactionRecord> getAllTransactions(UUID playerId);

    // ==================== 管理操作 ====================

    /**
     * 管理员设置余额
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param admin 操作管理员UUID
     * @param reason 原因
     */
    void adminSetBalance(UUID playerId, long amount, UUID admin, String reason);

    /**
     * 管理员给予金额
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param admin 操作管理员UUID
     * @param reason 原因
     */
    void adminGive(UUID playerId, long amount, UUID admin, String reason);

    /**
     * 管理员扣除金额
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param admin 操作管理员UUID
     * @param reason 原因
     * @return 如果扣除成功返回 true
     */
    boolean adminTake(UUID playerId, long amount, UUID admin, String reason);

    /**
     * 重置玩家账户
     * 
     * @param playerId 玩家UUID
     * @param reason 原因
     */
    void resetAccount(UUID playerId, String reason);

    // ==================== 异步操作 ====================

    /**
     * 异步获取余额
     * 
     * @param playerId 玩家UUID
     * @return 包含余额的 CompletableFuture
     */
    CompletableFuture<Long> getBalanceAsync(UUID playerId);

    /**
     * 异步存款
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param reason 原因
     * @return 包含操作结果的 CompletableFuture
     */
    CompletableFuture<Boolean> depositAsync(UUID playerId, long amount, String reason);

    /**
     * 异步取款
     * 
     * @param playerId 玩家UUID
     * @param amount 金额
     * @param reason 原因
     * @return 包含操作结果的 CompletableFuture
     */
    CompletableFuture<Boolean> withdrawAsync(UUID playerId, long amount, String reason);

    /**
     * 异步转账
     * 
     * @param from 付款方UUID
     * @param to 收款方UUID
     * @param amount 金额
     * @param reason 原因
     * @return 包含操作结果的 CompletableFuture
     */
    CompletableFuture<Boolean> transferAsync(UUID from, UUID to, long amount, String reason);

    // ==================== 统计信息 ====================

    /**
     * 获取银行总存款
     * 
     * @return 总存款
     */
    long getTotalDeposits();

    /**
     * 获取银行总贷款
     * 
     * @return 总贷款
     */
    long getTotalLoans();

    /**
     * 获取账户数量
     * 
     * @return 账户数量
     */
    int getAccountCount();

    /**
     * 获取默认余额
     * 
     * @return 默认余额
     */
    long getDefaultBalance();
}
