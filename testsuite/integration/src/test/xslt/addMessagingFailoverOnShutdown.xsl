<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:variable name="messaging" select="'urn:jboss:domain:messaging-activemq:'"/>

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="//*[local-name()='subsystem' and starts-with(namespace-uri(), $messaging)]
						  /*[local-name()='server']">
        <xsl:copy>
            <xsl:call-template name="copy-attributes"/>
            <xsl:element name="shared-store-master" namespace="{namespace-uri()}">
                <xsl:attribute name="failover-on-server-shutdown">true</xsl:attribute>
            </xsl:element>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- traverse the whole tree, so that all elements and attributes are eventually current node -->
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="copy-attributes">
        <xsl:for-each select="@*">
            <xsl:copy/>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
