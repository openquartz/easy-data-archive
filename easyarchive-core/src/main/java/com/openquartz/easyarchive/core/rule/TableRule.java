package com.openquartz.easyarchive.core.rule;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * archive table rule
 *
 * @author svnee
 */
@Getter
@Setter
public class TableRule {

    /**
     * rule id
     */
    private Integer id;

    /**
     * source table
     */
    private String sourceTable;

    /**
     * target table
     */
    private String targetTable;

    /**
     * 分组
     * 同一组的配置按照优先级串行执行
     */
    private String groupId;

    /**
     * 优先级，数字越小，优先级越高
     */
    private Integer priority;

    /**
     * 获取源数据sql，需要留两个占位符（?）传入开始时间和结束时间
     */
    private String fetchSql;

    /**
     * delete where
     */
    private String deleteWhere;

    /**
     * start time
     */
    private Date startTime;

    /**
     * keey day
     */
    private Integer keepDay = 20;

    /**
     * step
     */
    private Integer stepDay = 1;

    /**
     * step count
     */
    private Integer stepCount = 1000;

    /**
     * enable clean source
     */
    private Integer enableClean = 0;

    /**
     * enabled
     */
    private Integer enabled = 1;

    /**
     * 上锁时间
     */
    private Date lockTime;

    /**
     * 锁过期时间，单位ms
     */
    private Integer expire;

    public boolean valid() {
        return this.getEnabled() == 1 && (expire < 0 || (System.currentTimeMillis() - lockTime.getTime() > expire));
    }
}
