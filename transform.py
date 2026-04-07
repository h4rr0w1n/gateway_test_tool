import sys

with open('src/main/java/com/amhs/swim/test/testcase/SwimToAmhsTests.java', 'r') as f:
    text = f.read()

text = text.replace('"TEST.TOPIC"', 'TestConfig.getInstance().getProperty("gateway.default_topic")')
text = text.replace('"VVTSYMYX"', 'TestConfig.getInstance().getProperty("gateway.test_recipient")')

if 'import com.amhs.swim.test.config.TestConfig;' not in text:
    text = text.replace('import com.amhs.swim.test.util.Logger;', 'import com.amhs.swim.test.util.Logger;\nimport com.amhs.swim.test.config.TestConfig;')

with open('src/main/java/com/amhs/swim/test/testcase/SwimToAmhsTests.java', 'w') as f:
    f.write(text)
