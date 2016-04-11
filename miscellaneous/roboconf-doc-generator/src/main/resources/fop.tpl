<?xml version="1.0" encoding="UTF-8"?>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<fo:layout-master-set>
 		<fo:simple-page-master master-name="A4-portrait" 
                               page-height="29.7cm" page-width="21.0cm" margin="2cm">
    		<fo:region-body />
  		</fo:simple-page-master>
 	</fo:layout-master-set>
 	<fo:page-sequence master-reference="A4-portrait">
  		<fo:flow flow-name="xsl-region-body">
    		<fo:block >
  				<fo:external-graphic content-height="5em" content-width="5em" src="header.jpg"/>
     		</fo:block>
     		<fo:block text-align="center">${TITLE}</fo:block>
     		${CONTENT}
  		</fo:flow>
  
 	</fo:page-sequence>
</fo:root>
	