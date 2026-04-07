import sys

with open('src/main/java/com/amhs/swim/test/testcase/SwimToAmhsTests.java', 'r') as f:
    text = f.read()

text = text.replace('getProperty("gateway.default_topic")', 'getProperty("gateway.default_topic", "TEST.TOPIC")')
text = text.replace('getProperty("gateway.test_recipient")', 'getProperty("gateway.test_recipient", "VVTSYMYX")')

with open('src/main/java/com/amhs/swim/test/testcase/SwimToAmhsTests.java', 'w') as f:
    f.write(text)

with open('src/main/resources/config/test.properties', 'a') as f:
    f.write('\n# Default Target Settings\ngateway.default_topic=TEST.TOPIC\ngateway.test_recipient=VVTSYMYX\n')
