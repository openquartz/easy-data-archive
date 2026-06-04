package com.openquartz.easyarchive.core.expr.executors;

import com.openquartz.easyarchive.core.expr.cmd.Environment;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Result;
import com.openquartz.easyarchive.common.exception.Asserts;
import com.openquartz.easyarchive.common.exception.CommonErrorCode;
import com.openquartz.easyarchive.common.exception.EasyArchiveException;
import com.openquartz.easyarchive.common.util.StringUtils;
import com.openquartz.easyarchive.core.exception.CoreErrorCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 字符串随机
 * 语法：{rand_c 随机长度 随机字符范围类型}
 *
 * @author svnee
 */
public class RandomAlphaNumExecutor implements CommandExecutor {

    private static final List<Character> UP_CASE_SET;
    private static final List<Character> UP_CASE_IGNORE_I_SET;
    private static final List<Character> LOWER_CASE_SET;
    private static final List<Character> NUMBER_SET;
    private static final List<Character> TOTAL_SET;

    private static final Random RANDOM = new Random();

    static {
        UP_CASE_SET = Arrays.asList(
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z'
        );
        UP_CASE_IGNORE_I_SET = Arrays.asList(
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'J', 'K', 'L', 'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z'
        );
        LOWER_CASE_SET = Arrays.asList(
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
            'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
            'y', 'z'
        );
        NUMBER_SET = Arrays.asList(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        );
        TOTAL_SET = new ArrayList<>();
        TOTAL_SET.addAll(UP_CASE_SET);
        TOTAL_SET.addAll(LOWER_CASE_SET);
        TOTAL_SET.addAll(NUMBER_SET);
    }

    @Override
    public Result exec(Command command) {
        int count = Integer.parseInt(command.getFirstParam());
        StringBuilder sb = new StringBuilder(count);
        String type = command.getSecondParam();
        for (int i = 1; i <= count; i++) {
            sb.append(randGetCharacter(type));
        }
        return Result.success(sb.toString());
    }

    private Character randGetCharacter(String type) {
        List<Character> characterSet = CharacterClass.resolveCharacters(type);
        int index = RANDOM.nextInt(characterSet.size());
        return characterSet.get(index);
    }

    @Override
    public void validate(Command command) {
        Asserts.notNull(command, CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.notNull(command.getFirstParam(), CommonErrorCode.PARAM_ILLEGAL_ERROR);
        Asserts.isTrue(StringUtils.isNumeric(command.getFirstParam()), CommonErrorCode.PARAM_ILLEGAL_ERROR);
    }

    @Override
    public void init(Environment environment) {

    }

    private enum CharacterClass {
        UPPERCASE_NO_I("UPPERCASE_NO_I", UP_CASE_IGNORE_I_SET),
        UPPERCASE("UPPERCASE", UP_CASE_SET),
        LOWERCASE("LOWERCASE", LOWER_CASE_SET),
        NUMBER("NUMBER", NUMBER_SET);

        private final String code;
        private final List<Character> characters;

        CharacterClass(String code, List<Character> characters) {
            this.code = code;
            this.characters = characters;
        }

        private static List<Character> resolveCharacters(String code) {
            if (StringUtils.isBlank(code)) {
                return TOTAL_SET;
            }
            return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .map(item -> item.characters)
                .orElseThrow(() -> EasyArchiveException.withPlaceholders(
                    CoreErrorCode.RANDOM_ALPHA_NUM_TYPE_UNSUPPORTED, code));
        }
    }
}
