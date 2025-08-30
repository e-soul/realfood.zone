package zone.realfood;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.constructs.Construct;

public class DnsAndCert extends Stack {

    private final IHostedZone zone;
    private final String subdomain;
    private final Certificate certificate;

    public DnsAndCert(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.zone = HostedZone.fromLookup(this, "RealfoodZone", HostedZoneProviderProps.builder().domainName("realfood.zone").privateZone(false).build());

        this.subdomain = "beta.realfood.zone";

        this.certificate = Certificate.Builder.create(this, "BetaApiCert").domainName(this.subdomain).validation(CertificateValidation.fromDns(this.zone))
                .build();
    }

    public IHostedZone getZone() {
        return this.zone;
    }

    public String getSubdomain() {
        return this.subdomain;
    }

    public Certificate getCertificate() {
        return this.certificate;
    }
}
