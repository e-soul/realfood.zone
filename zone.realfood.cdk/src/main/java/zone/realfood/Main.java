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

        new StaticContent(app, "StaticContent", envProps);

        DnsAndCert dns = new DnsAndCert(app, "DnsAndCert", envProps);

        UserProfile userProfile = new UserProfile(app, "UserProfile", envProps);

        Backend api = new Backend(app, "Backend", envProps, dns.getZone(), dns.getCertificate(), dns.getSubdomain(), userProfile.getTable());
        api.addDependency(dns);
        api.addDependency(userProfile);

        app.synth();
    }
}
