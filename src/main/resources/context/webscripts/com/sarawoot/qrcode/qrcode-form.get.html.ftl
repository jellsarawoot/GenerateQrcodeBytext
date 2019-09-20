<div id="qrcodeDialog-qrcodeDialog">
    <div class="hd">${msg("header.qrcode.title")}</div>
    <div class="bd">
        <form id="qrcodeDialog-form" action="" method="get">
        
        	<input type="hidden" name="sourcenodeRef" id="sourcenodeRef" value=""/>
            <div class="form-container yui-form-qrcode" style="padding:15px 20px;">

                <div class="yui-g">
                    <div class="yui-u first">

                        <div class="form-field form-field-qrcode">
                            <div class="yui-field-qrcode"><label for="qrcode.title">${msg("qrcode.title.name")} :</label></div>
                            <div class = "qrcode-form-item">
                                <textarea id="qrcode-title" name="qrcodetitle" rows="3" cols="60" tabindex="0" ></textarea>
                                
                            	<span class="help-icon" onclick="showHelp('qrcode-help1')">
									<img id="qrcode-title-help-icon" style="vertical-align:0px;" tabindex="0" title="Click to show and hide help text for the field." src="/share/res/components/form/images/help.png">
								</span>
                            </div>
                            <div style="clear:both;"></div>
                        </div>
                        <div id="qrcode-help1" class="qrcode-help-text" style="display: none;">${msg("label.help.qrcode-title")}</div>
                        
                       
                       <div class="form-field form-field-qrcode">
                            <div class="yui-field-qrcode "><label for="text2pdf.size">${msg("qrcode.size.name")} :</label></div>
                            <div class = "qrcode-form-item">
                                <select id="qrcode-size" name="qrcodesize" size="1"  value="10">
                                    
                                    <option value="100">100x100</option>
                                    <option value="200" >200x200</option>
                                    <option value="300" selected="selected">300x300</option>
                                    <option value="400">400x400</option>
                                    <option value="500">500x500</option>
                                   
                                </select>
                                <span class="help-icon" onclick="showHelp('qrcode-help2')">
									<img id="qrcode-size-help-icon" style="vertical-align:0px;" tabindex="0" title="Click to show and hide help text for the field." src="/share/res/components/form/images/help.png">
								</span>
                            </div>
                            <div style="clear:both;"></div>
                        </div>
                        <div id="qrcode-help2" class="qrcode-help-text" style="display: none;">${msg("label.help.qrcode-size")}</div>

                    </div>
                </div>
            </div>
            <div class="bdft">
                <input type="button" id="qrcodeDialog-ok" value="${msg("qrcode.button.submit")}" />
                <input type="button" id="qrcodeDialog-cancel" value="${msg("qrcode.button.cancel")}" />
            </div>
    </div>
    </form>
</div>
</div>