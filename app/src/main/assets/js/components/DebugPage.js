"use strict";

function DebugPage({openView,setPageTitle}) {
    return RE.Container.col.top.center({style:{marginTop:'200px'}},{},
        RE.Button({variant:"contained", onClick: () => be.debug()}, 'Debug'),
        RE.Button({variant:"contained", onClick: () => openView(HOME_PAGE_VIEW)}, 'Go to home page'),
    )
}
