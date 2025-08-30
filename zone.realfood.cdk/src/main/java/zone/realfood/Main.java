package zone.realfood;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class Main {

    public static void main(final String[] args) {
        App app = new App();

        String account = System.getenv("CDK_DEFAULT_ACCOUNT");
        String region = System.getenv("CDK_DEFAULT_REGION");

        Environment env = Environment.builder().account(account).region(region).build();
        StackProps envProps = StackProps.builder().env(env).build();

        DnsAndCert dns = new DnsAndCert(app, "DnsAndCert", envProps);

        Backend api = new Backend(app, "HelloStack", envProps, dns.getZone(), dns.getCertificate(), dns.getSubdomain());
        api.addDependency(dns);

        app.synth();
    }
}
