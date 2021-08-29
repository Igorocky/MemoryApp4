"use strict";

const VIEW_NAME_ATTR = '_view'
function createQueryObjectForView(viewName, params) {
    return {[VIEW_NAME_ATTR]:viewName, ...(hasValue(params)?params:{})}
}

const HOME_PAGE_VIEW = 'homePage'
const DEBUG_VIEW = 'debug'
const PAGE_1_VIEW = 'page1'
const PAGE_2_VIEW = 'page2'
const TAGS_VIEW = 'tags'
const VIEWS = {}
function addView({name, component}) {
    VIEWS[name] = {
        name,
        render({query, openView, setPageTitle}) {
            return re(component,{openView, setPageTitle, query})
        }
    }
}
addView({name: HOME_PAGE_VIEW, component: HomePage})
addView({name: DEBUG_VIEW, component: DebugPage})
addView({name: PAGE_1_VIEW, component: Page1})
addView({name: PAGE_2_VIEW, component: Page2})
addView({name: TAGS_VIEW, component: TagsView})

const ViewSelector = ({}) => {
    const [currentViewUrl, setCurrentViewUrl] = useState(null)
    const [environmentName, setEnvironmentName] = useState(null)
    const [pageTitle, setPageTitle] = useState(null)
    const [showMoreControlButtons, setShowMoreControlButtons] = useState(false)

    const query = parseSearchParams(currentViewUrl)

    useEffect(() => {
        updatePageTitle()
    }, [environmentName, pageTitle])

    useEffect(() => {
        openView(TAGS_VIEW)
    }, [])

    function updatePageTitle() {
        document.title = `${environmentName == 'PROD' ? '' : '{' + environmentName + '} - '}${pageTitle}`
    }

    function openView(viewName,params) {
        setCurrentViewUrl(window.location.pathname + '?' + new URLSearchParams(createQueryObjectForView(viewName,params)).toString())
    }

    function getSelectedView() {
        return VIEWS[query[VIEW_NAME_ATTR]]
    }

    function renderSelectedView() {
        const selectedView = getSelectedView()
        if (selectedView) {
            return selectedView.render({
                query,
                openView,
                setPageTitle: str => setPageTitle(str),
            })
        }
    }

    function renderControlButtons() {
        const selectedViewName = getSelectedView()?.name
        const bgColor = viewName => viewName == selectedViewName ? '#00ff72' : undefined
        const buttons = [[
            {iconName:"sell", onClick: () => openView(TAGS_VIEW), style:{backgroundColor:bgColor(TAGS_VIEW)}},
            {symbol:"1", onClick: () => openView(PAGE_1_VIEW), style:{backgroundColor:bgColor(PAGE_1_VIEW)}},
            {symbol:"2", onClick: () => openView(PAGE_2_VIEW), style:{backgroundColor:bgColor(PAGE_2_VIEW)}},
            {iconName:"adb", onClick: () => openView(DEBUG_VIEW), style:{backgroundColor:bgColor(DEBUG_VIEW)}},
            {iconName:"more_horiz", onClick: () => setShowMoreControlButtons(old => !old)},
        ]]
        if (showMoreControlButtons) {
            buttons.push([
                {symbol:"?", onClick: () => openView(PAGE_1_VIEW), style:{backgroundColor:bgColor(PAGE_1_VIEW)}},
                {symbol:"?", onClick: () => openView(PAGE_1_VIEW), style:{backgroundColor:bgColor(PAGE_1_VIEW)}},
                {symbol:"?", onClick: () => openView(PAGE_1_VIEW), style:{backgroundColor:bgColor(PAGE_1_VIEW)}},
            ])
        }

        return re(KeyPad, {
            componentKey: "controlButtons",
            keys: buttons,
            variant: "outlined",
        })
    }

    if (currentViewUrl) {
        return RE.Container.col.top.left({}, {},
            renderControlButtons(),
            renderSelectedView()
        )
    } else {
        const newViewUrl = window.location.pathname + window.location.search
        setCurrentViewUrl(newViewUrl)
        return "Starting..."
    }
}